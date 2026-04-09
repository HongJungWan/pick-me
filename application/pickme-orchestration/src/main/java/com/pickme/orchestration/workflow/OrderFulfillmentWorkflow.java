package com.pickme.orchestration.workflow;

import com.pickme.orchestration.dto.OrderFulfillmentRequest;
import com.pickme.orchestration.dto.OrderFulfillmentResult;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * 주문 이행 사가 워크플로우.
 * 재고 예약 → 결제 처리 → 주문 확정 → 재고 확정 순서로 실행하며,
 * 실패 시 보상 트랜잭션을 명시적으로 수행한다.
 */
@WorkflowInterface
public interface OrderFulfillmentWorkflow {

    @WorkflowMethod
    OrderFulfillmentResult execute(OrderFulfillmentRequest request);

    @QueryMethod
    String getStatus();

    @SignalMethod
    void cancelByAdmin(String reason);
}
