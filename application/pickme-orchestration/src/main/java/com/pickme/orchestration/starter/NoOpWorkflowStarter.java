package com.pickme.orchestration.starter;

import com.pickme.orchestration.dto.OrderLineItem;
import com.pickme.orchestration.port.WorkflowStarter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Temporal 비활성화 시 사용되는 NoOp 폴백.
 * 기존 Kafka 코레오그래피만으로 동작할 때 워크플로우 시작 요청을 무시한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "pickme.temporal.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpWorkflowStarter implements WorkflowStarter {

    @Override
    public void startOrderFulfillment(UUID orderId, UUID ordererId,
                                      List<OrderLineItem> orderLines, long totalAmount, String paymentMethod) {
        log.debug("Temporal 비활성화 — 워크플로우 시작 생략: orderId={}", orderId);
    }

    @Override
    public void startRefund(UUID orderId, String reason, long refundAmount) {
        log.debug("Temporal 비활성화 — 환불 워크플로우 시작 생략: orderId={}", orderId);
    }
}
