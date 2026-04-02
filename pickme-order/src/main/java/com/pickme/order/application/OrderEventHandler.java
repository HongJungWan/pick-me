package com.pickme.order.application;

import com.pickme.common.event.DomainEventPublisher;
import com.pickme.common.idempotency.IdempotencyFilter;
import com.pickme.order.domain.model.Order;
import com.pickme.order.domain.model.OrderId;
import com.pickme.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventHandler {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;
    private final IdempotencyFilter idempotencyFilter;

    @Transactional
    public void handlePaymentCompleted(UUID eventId, UUID orderId) {
        if (idempotencyFilter.isDuplicate(eventId)) {
            log.info("중복 이벤트 무시: eventId={}", eventId);
            return;
        }

        Order order = orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

        order.completePayment();
        orderRepository.save(order);
        eventPublisher.publishAll(order);

        idempotencyFilter.markProcessed(eventId, "PaymentCompletedEvent");
        log.info("주문 확정: orderId={}", orderId);
    }

    @Transactional
    public void handlePaymentFailed(UUID eventId, UUID orderId, String reason) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        Order order = orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

        order.cancel("결제 실패: " + reason);
        orderRepository.save(order);
        eventPublisher.publishAll(order);

        idempotencyFilter.markProcessed(eventId, "PaymentFailedEvent");
        log.info("주문 취소 (결제 실패): orderId={}", orderId);
    }

    @Transactional
    public void handleInventoryShortage(UUID eventId, UUID orderId) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        Order order = orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

        order.cancel("재고 부족");
        orderRepository.save(order);
        eventPublisher.publishAll(order);

        idempotencyFilter.markProcessed(eventId, "InventoryShortageEvent");
        log.info("주문 취소 (재고 부족): orderId={}", orderId);
    }
}
