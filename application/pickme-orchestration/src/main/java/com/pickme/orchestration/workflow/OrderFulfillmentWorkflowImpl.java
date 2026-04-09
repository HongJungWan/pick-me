package com.pickme.orchestration.workflow;

import com.pickme.orchestration.activity.OrderActivities;
import com.pickme.orchestration.dto.OrderFulfillmentRequest;
import com.pickme.orchestration.dto.OrderFulfillmentResult;
import com.pickme.orchestration.dto.PaymentResult;
import com.pickme.orchestration.dto.ReserveResult;
import com.pickme.orchestration.dto.WorkflowStepStatus;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class OrderFulfillmentWorkflowImpl implements OrderFulfillmentWorkflow {

    // 내부 서비스 호출용 (재고/주문 확정 — DB 직접 접근, 빠른 응답 기대)
    private final OrderActivities internalActivities = Workflow.newActivityStub(
            OrderActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(5)
                            .setInitialInterval(Duration.ofMillis(500))
                            .setBackoffCoefficient(2.0)
                            .build())
                    .build()
    );

    // 외부 PG 호출용 (결제 — 네트워크 지연 허용, 보수적 재시도)
    private final OrderActivities paymentActivities = Workflow.newActivityStub(
            OrderActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(60))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(2))
                            .setBackoffCoefficient(3.0)
                            .setDoNotRetry(IllegalArgumentException.class.getName())
                            .build())
                    .build()
    );

    private WorkflowStepStatus status = WorkflowStepStatus.STARTED;
    private boolean cancelRequested = false;
    private String cancelReason = null;

    @Override
    public OrderFulfillmentResult execute(OrderFulfillmentRequest request) {
        // 버전 게이트: 첫 프로덕션 배포 후 워크플로우 변경 시 안전한 마이그레이션을 위한 구조
        int version = Workflow.getVersion("v1-initial", Workflow.DEFAULT_VERSION, 1);

        // Step 1: 재고 예약
        status = WorkflowStepStatus.RESERVING_INVENTORY;
        ReserveResult reserveResult;
        try {
            reserveResult = internalActivities.reserveInventory(request.orderId(), request.orderLines());
        } catch (ActivityFailure e) {
            status = WorkflowStepStatus.INVENTORY_FAILED;
            boolean compensationFailed = !safeCancel(request, "재고 예약 실패");
            return compensationFailed
                    ? OrderFulfillmentResult.failedWithCompensationError(request.orderId(), "INVENTORY_FAILURE", e.getMessage())
                    : OrderFulfillmentResult.failed(request.orderId(), "INVENTORY_FAILURE", e.getMessage());
        }

        if (!reserveResult.success()) {
            status = WorkflowStepStatus.INVENTORY_SHORTAGE;
            boolean compensationFailed = !safeCancel(request, reserveResult.failureReason());
            return compensationFailed
                    ? OrderFulfillmentResult.failedWithCompensationError(request.orderId(), "INVENTORY_SHORTAGE", reserveResult.failureReason())
                    : OrderFulfillmentResult.failed(request.orderId(), "INVENTORY_SHORTAGE", reserveResult.failureReason());
        }

        // 관리자 취소 체크
        if (cancelRequested) {
            return handleAdminCancel(request, true);
        }

        // Step 2: 결제 처리 (실패 시 재고 보상)
        status = WorkflowStepStatus.PROCESSING_PAYMENT;
        PaymentResult paymentResult;
        try {
            paymentResult = paymentActivities.processPayment(
                    request.orderId(), request.ordererId(),
                    request.totalAmount(), request.paymentMethod());
        } catch (ActivityFailure e) {
            status = WorkflowStepStatus.PAYMENT_FAILED;
            boolean compensationFailed = !safeRestoreAndCancel(request, "결제 처리 실패");
            return compensationFailed
                    ? OrderFulfillmentResult.failedWithCompensationError(request.orderId(), "PAYMENT_FAILURE", e.getMessage())
                    : OrderFulfillmentResult.failed(request.orderId(), "PAYMENT_FAILURE", e.getMessage());
        }

        if (!paymentResult.success()) {
            status = WorkflowStepStatus.PAYMENT_DECLINED;
            boolean compensationFailed = !safeRestoreAndCancel(request, "결제 거절: " + paymentResult.failureReason());
            return compensationFailed
                    ? OrderFulfillmentResult.failedWithCompensationError(request.orderId(), "PAYMENT_DECLINED", paymentResult.failureReason())
                    : OrderFulfillmentResult.failed(request.orderId(), "PAYMENT_DECLINED", paymentResult.failureReason());
        }

        // 관리자 취소 체크
        if (cancelRequested) {
            return handleAdminCancel(request, true);
        }

        // Step 3: 주문 확정
        status = WorkflowStepStatus.CONFIRMING_ORDER;
        try {
            internalActivities.confirmOrder(request.orderId());
        } catch (ActivityFailure e) {
            // 결제 성공 후 주문 확정 실패 → 환불 + 재고 복원 + 주문 취소
            boolean compensationFailed = !safeRefundAndRestoreAndCancel(request, "주문 확정 실패");
            return compensationFailed
                    ? OrderFulfillmentResult.failedWithCompensationError(request.orderId(), "CONFIRM_ORDER_FAILURE", e.getMessage())
                    : OrderFulfillmentResult.failed(request.orderId(), "CONFIRM_ORDER_FAILURE", e.getMessage());
        }

        // Step 4: 재고 확정
        status = WorkflowStepStatus.CONFIRMING_INVENTORY;
        try {
            internalActivities.confirmInventory(request.orderId(), request.orderLines());
        } catch (ActivityFailure e) {
            // 주문/결제는 이미 확정 → 재고만 reserved 상태로 유지, 수동 조치 필요
            Workflow.getLogger(OrderFulfillmentWorkflowImpl.class)
                    .error("재고 확정 실패 — 수동 조치 필요: orderId={}", request.orderId(), e);
        }

        status = WorkflowStepStatus.COMPLETED;
        return OrderFulfillmentResult.success(request.orderId(), paymentResult.paymentId());
    }

    @Override
    public String getStatus() {
        return status.name();
    }

    @Override
    public void cancelByAdmin(String reason) {
        this.cancelRequested = true;
        this.cancelReason = reason;
    }

    private OrderFulfillmentResult handleAdminCancel(OrderFulfillmentRequest request, boolean inventoryReserved) {
        if (inventoryReserved) {
            safeRestoreAndCancel(request, "관리자 취소: " + cancelReason);
        } else {
            safeCancel(request, "관리자 취소: " + cancelReason);
        }
        return OrderFulfillmentResult.failed(request.orderId(), "ADMIN_CANCELLED", cancelReason);
    }

    /** @return true if compensation succeeded, false if it failed */
    private boolean safeCancel(OrderFulfillmentRequest request, String reason) {
        try {
            internalActivities.cancelOrder(request.orderId(), reason);
            return true;
        } catch (Exception e) {
            Workflow.getLogger(OrderFulfillmentWorkflowImpl.class)
                    .error("주문 취소 보상 실패: orderId={}", request.orderId(), e);
            return false;
        }
    }

    /** @return true if compensation succeeded, false if it failed */
    private boolean safeRestoreAndCancel(OrderFulfillmentRequest request, String reason) {
        boolean restoreSuccess = true;
        try {
            internalActivities.restoreInventory(request.orderId(), request.orderLines());
        } catch (Exception e) {
            Workflow.getLogger(OrderFulfillmentWorkflowImpl.class)
                    .error("재고 복원 보상 실패: orderId={}", request.orderId(), e);
            restoreSuccess = false;
        }
        boolean cancelSuccess = safeCancel(request, reason);
        return restoreSuccess && cancelSuccess;
    }

    /** @return true if compensation succeeded, false if it failed */
    private boolean safeRefundAndRestoreAndCancel(OrderFulfillmentRequest request, String reason) {
        boolean refundSuccess = true;
        try {
            paymentActivities.processPayment(
                    request.orderId(), request.ordererId(), request.totalAmount(), "REFUND");
        } catch (Exception e) {
            Workflow.getLogger(OrderFulfillmentWorkflowImpl.class)
                    .error("결제 환불 보상 실패: orderId={}", request.orderId(), e);
            refundSuccess = false;
        }
        boolean restoreAndCancelSuccess = safeRestoreAndCancel(request, reason);
        return refundSuccess && restoreAndCancelSuccess;
    }
}
