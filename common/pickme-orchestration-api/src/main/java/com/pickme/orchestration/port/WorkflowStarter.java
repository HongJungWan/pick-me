package com.pickme.orchestration.port;

import com.pickme.orchestration.dto.OrderLineItem;

import java.util.List;
import java.util.UUID;

/**
 * 워크플로우 시작 포트.
 * 도메인 모듈이 Temporal SDK에 직접 의존하지 않고 워크플로우를 시작할 수 있도록 한다.
 */
public interface WorkflowStarter {

    void startOrderFulfillment(UUID orderId, UUID ordererId,
                               List<OrderLineItem> orderLines, long totalAmount, String paymentMethod);

    void startRefund(UUID orderId, String reason, long refundAmount);
}
