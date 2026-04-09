package com.pickme.orchestration.activity;

import com.pickme.orchestration.dto.OrderLineItem;
import com.pickme.orchestration.dto.PaymentResult;
import com.pickme.orchestration.dto.ReserveResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Shadow Mode Activity 구현체.
 * 실제 상태 변경 없이 dry-run 검증만 수행하고 결과를 로깅한다.
 * Kafka 코레오그래피가 실제 상태를 관리하므로 충돌 없음.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "pickme.temporal.shadow-mode", havingValue = "true")
public class ShadowOrderActivitiesImpl implements OrderActivities {

    @Override
    public ReserveResult reserveInventory(UUID orderId, List<OrderLineItem> items) {
        log.info("[Shadow] 재고 예약 검증 (dry-run): orderId={}, items={}", orderId, items.size());
        return ReserveResult.success(orderId);
    }

    @Override
    public void confirmInventory(UUID orderId, List<OrderLineItem> items) {
        log.info("[Shadow] 재고 확정 검증 (dry-run): orderId={}", orderId);
    }

    @Override
    public void restoreInventory(UUID orderId, List<OrderLineItem> items) {
        log.info("[Shadow] 재고 복원 검증 (dry-run): orderId={}", orderId);
    }

    @Override
    public PaymentResult processPayment(UUID orderId, UUID ordererId, long amount, String paymentMethod) {
        log.info("[Shadow] 결제 처리 검증 (dry-run): orderId={}, amount={}", orderId, amount);
        return PaymentResult.success(UUID.randomUUID());
    }

    @Override
    public void confirmOrder(UUID orderId) {
        log.info("[Shadow] 주문 확정 검증 (dry-run): orderId={}", orderId);
    }

    @Override
    public void cancelOrder(UUID orderId, String reason) {
        log.info("[Shadow] 주문 취소 검증 (dry-run): orderId={}, reason={}", orderId, reason);
    }
}
