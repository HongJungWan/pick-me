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
 * Temporal Activity 구현체.
 * CommandPort 인터페이스를 통해 각 도메인 모듈의 Application Service에 위임한다.
 * Temporal SDK 의존성은 이 모듈에만 격리된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderActivitiesImpl implements OrderActivities {

    private final InventoryCommandPort inventoryCommandPort;
    private final PaymentCommandPort paymentCommandPort;
    private final OrderCommandPort orderCommandPort;

    @Override
    public ReserveResult reserveInventory(UUID orderId, List<OrderLineItem> items) {
        log.info("[Activity] 재고 예약 시작: orderId={}, items={}", orderId, items.size());
        return inventoryCommandPort.reserveInventory(orderId, items);
    }

    @Override
    public void confirmInventory(UUID orderId, List<OrderLineItem> items) {
        log.info("[Activity] 재고 확정: orderId={}", orderId);
        inventoryCommandPort.confirmInventory(orderId, items);
    }

    @Override
    public void restoreInventory(UUID orderId, List<OrderLineItem> items) {
        log.info("[Activity] 재고 복원 (보상): orderId={}", orderId);
        inventoryCommandPort.restoreInventory(orderId, items);
    }

    @Override
    public PaymentResult processPayment(UUID orderId, UUID ordererId, long amount, String paymentMethod) {
        log.info("[Activity] 결제 처리: orderId={}, amount={}", orderId, amount);
        return paymentCommandPort.processPayment(orderId, ordererId, amount, paymentMethod);
    }

    @Override
    public void confirmOrder(UUID orderId) {
        log.info("[Activity] 주문 확정: orderId={}", orderId);
        orderCommandPort.confirmOrder(orderId);
    }

    @Override
    public void cancelOrder(UUID orderId, String reason) {
        log.info("[Activity] 주문 취소 (보상): orderId={}, reason={}", orderId, reason);
        orderCommandPort.cancelOrder(orderId, reason);
    }
}
