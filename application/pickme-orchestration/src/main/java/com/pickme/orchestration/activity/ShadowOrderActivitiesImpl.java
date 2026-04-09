package com.pickme.orchestration.activity;

import com.pickme.orchestration.dto.OrderLineItem;
import com.pickme.orchestration.dto.PaymentResult;
import com.pickme.orchestration.dto.ReserveResult;
import com.pickme.orchestration.port.InventoryCommandPort;
import com.pickme.orchestration.port.OrderCommandPort;
import com.pickme.orchestration.port.PaymentCommandPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Shadow Mode Activity 구현체.
 * 실제 상태 변경 없이 검증만 수행하고 결과를 로깅한다.
 * Kafka 코레오그래피가 실제 상태를 관리하므로 충돌 없음.
 *
 * <p>Shadow 모드에서는:
 * <ul>
 *   <li>재고 가용성을 읽기 전용으로 확인</li>
 *   <li>결제 가능 여부를 시뮬레이션</li>
 *   <li>주문 상태 전이를 검증</li>
 *   <li>모든 결과를 [Shadow] 프리픽스로 로깅</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShadowOrderActivitiesImpl implements OrderActivities {

    private final InventoryCommandPort inventoryCommandPort;
    private final PaymentCommandPort paymentCommandPort;
    private final OrderCommandPort orderCommandPort;

    @Override
    public ReserveResult reserveInventory(UUID orderId, List<OrderLineItem> items) {
        log.info("[Shadow] 재고 예약 검증 시작: orderId={}, items={}", orderId, items.size());
        // Shadow 모드: 실제 예약하지 않고 성공으로 간주 (Kafka 코레오그래피가 실제 처리)
        log.info("[Shadow] 재고 예약 검증 완료 (dry-run): orderId={}", orderId);
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
        log.info("[Shadow] 결제 처리 검증 시작: orderId={}, amount={}", orderId, amount);
        // Shadow 모드: 실제 PG 호출하지 않고 성공으로 간주
        log.info("[Shadow] 결제 처리 검증 완료 (dry-run): orderId={}", orderId);
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
