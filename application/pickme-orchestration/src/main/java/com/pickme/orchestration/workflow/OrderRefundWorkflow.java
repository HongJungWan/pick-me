package com.pickme.orchestration.workflow;

import com.pickme.orchestration.dto.RefundRequest;
import com.pickme.orchestration.dto.RefundResult;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * 주문 환불 워크플로우.
 * 환불 요청 → PG 환불 → 재고 복원 → 환불 완료 순서로 실행.
 */
@WorkflowInterface
public interface OrderRefundWorkflow {

    @WorkflowMethod
    RefundResult execute(RefundRequest request);

    @QueryMethod
    String getStatus();
}
