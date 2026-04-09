package com.pickme.orchestration.dto;

/**
 * 주문 이행 워크플로우의 단계별 상태.
 * Temporal Query 메서드를 통해 외부에서 조회 가능.
 */
public enum WorkflowStepStatus {
    STARTED,
    RESERVING_INVENTORY,
    INVENTORY_FAILED,
    INVENTORY_SHORTAGE,
    PROCESSING_PAYMENT,
    PAYMENT_FAILED,
    PAYMENT_DECLINED,
    CONFIRMING_ORDER,
    CONFIRMING_INVENTORY,
    COMPLETED
}
