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

    private final OrderActivities activities = Workflow.newActivityStub(
            OrderActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setBackoffCoefficient(2.0)
                            .build())
                    .build()
    );

    private WorkflowStepStatus status = WorkflowStepStatus.STARTED;

    @Override
    public OrderFulfillmentResult execute(OrderFulfillmentRequest request) {
        // Step 1: 재고 예약
        status = WorkflowStepStatus.RESERVING_INVENTORY;
        ReserveResult reserveResult;
        try {
            reserveResult = activities.reserveInventory(request.orderId(), request.orderLines());
        } catch (ActivityFailure e) {
            status = WorkflowStepStatus.INVENTORY_FAILED;
            safeCancel(request, "재고 예약 실패");
            return OrderFulfillmentResult.failed(request.orderId(), "INVENTORY_FAILURE", e.getMessage());
        }

        if (!reserveResult.success()) {
            status = WorkflowStepStatus.INVENTORY_SHORTAGE;
            safeCancel(request, reserveResult.failureReason());
            return OrderFulfillmentResult.failed(request.orderId(), "INVENTORY_SHORTAGE", reserveResult.failureReason());
        }

        // Step 2: 결제 처리 (실패 시 재고 보상)
        status = WorkflowStepStatus.PROCESSING_PAYMENT;
        PaymentResult paymentResult;
        try {
            paymentResult = activities.processPayment(
                    request.orderId(), request.ordererId(),
                    request.totalAmount(), request.paymentMethod());
        } catch (ActivityFailure e) {
            status = WorkflowStepStatus.PAYMENT_FAILED;
            safeRestoreAndCancel(request, "결제 처리 실패");
            return OrderFulfillmentResult.failed(request.orderId(), "PAYMENT_FAILURE", e.getMessage());
        }

        if (!paymentResult.success()) {
            status = WorkflowStepStatus.PAYMENT_DECLINED;
            safeRestoreAndCancel(request, "결제 거절: " + paymentResult.failureReason());
            return OrderFulfillmentResult.failed(request.orderId(), "PAYMENT_DECLINED", paymentResult.failureReason());
        }

        // Step 3: 주문 확정
        status = WorkflowStepStatus.CONFIRMING_ORDER;
        activities.confirmOrder(request.orderId());

        // Step 4: 재고 확정
        status = WorkflowStepStatus.CONFIRMING_INVENTORY;
        activities.confirmInventory(request.orderId(), request.orderLines());

        status = WorkflowStepStatus.COMPLETED;
        return OrderFulfillmentResult.success(request.orderId(), paymentResult.paymentId());
    }

    @Override
    public String getStatus() {
        return status.name();
    }

    private void safeCancel(OrderFulfillmentRequest request, String reason) {
        try {
            activities.cancelOrder(request.orderId(), reason);
        } catch (Exception e) {
            Workflow.getLogger(OrderFulfillmentWorkflowImpl.class)
                    .warn("주문 취소 보상 실패: orderId={}", request.orderId(), e);
        }
    }

    private void safeRestoreAndCancel(OrderFulfillmentRequest request, String reason) {
        try {
            activities.restoreInventory(request.orderId(), request.orderLines());
        } catch (Exception e) {
            Workflow.getLogger(OrderFulfillmentWorkflowImpl.class)
                    .warn("재고 복원 보상 실패: orderId={}", request.orderId(), e);
        }
        safeCancel(request, reason);
    }
}
