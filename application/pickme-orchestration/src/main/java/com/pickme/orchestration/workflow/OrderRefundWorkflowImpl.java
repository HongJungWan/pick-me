package com.pickme.orchestration.workflow;

import com.pickme.orchestration.activity.RefundActivities;
import com.pickme.orchestration.dto.RefundRequest;
import com.pickme.orchestration.dto.RefundResult;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class OrderRefundWorkflowImpl implements OrderRefundWorkflow {

    private final RefundActivities activities = Workflow.newActivityStub(
            RefundActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(60))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(2))
                            .setBackoffCoefficient(2.0)
                            .build())
                    .build()
    );

    private String status = "STARTED";

    @Override
    public RefundResult execute(RefundRequest request) {
        // Step 1: 주문 상태를 REFUND_REQUESTED로 전이
        status = "REQUESTING_REFUND";
        try {
            activities.requestRefund(request.orderId(), request.reason());
        } catch (ActivityFailure e) {
            status = "REQUEST_FAILED";
            return RefundResult.failed(request.orderId(), "환불 요청 실패: " + e.getMessage());
        }

        // Step 2: PG 환불 처리
        status = "PROCESSING_REFUND";
        try {
            activities.processRefund(request.orderId(), request.refundAmount());
        } catch (ActivityFailure e) {
            status = "REFUND_FAILED";
            return RefundResult.failed(request.orderId(), "PG 환불 처리 실패: " + e.getMessage());
        }

        // Step 3: 재고 복원
        status = "RESTORING_INVENTORY";
        try {
            activities.restoreInventory(request.orderId(), request.orderLines());
        } catch (ActivityFailure e) {
            Workflow.getLogger(OrderRefundWorkflowImpl.class)
                    .error("재고 복원 실패 — 수동 조치 필요: orderId={}", request.orderId(), e);
        }

        // Step 4: 환불 완료 상태 전이
        status = "COMPLETING_REFUND";
        try {
            activities.completeRefund(request.orderId());
        } catch (ActivityFailure e) {
            Workflow.getLogger(OrderRefundWorkflowImpl.class)
                    .error("환불 완료 상태 전이 실패: orderId={}", request.orderId(), e);
        }

        status = "COMPLETED";
        return RefundResult.success(request.orderId());
    }

    @Override
    public String getStatus() {
        return status;
    }
}
