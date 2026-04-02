package com.pickme.order.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickme.common.event.DomainEvent;
import com.pickme.common.idempotency.IdempotencyFilter;
import com.pickme.common.outbox.OutboxEvent;
import com.pickme.common.outbox.OutboxRepository;
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
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final IdempotencyFilter idempotencyFilter;

    @Transactional
    public void handlePaymentCompleted(UUID eventId, UUID orderId) {
        if (idempotencyFilter.isDuplicate(eventId)) {
            log.info("중복 이벤트 무시: eventId={}", eventId);
            return;
        }

        Order order = orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

        order.markPaymentPending();
        order.confirm();
        orderRepository.save(order);
        publishDomainEvents(order);

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
        publishDomainEvents(order);

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
        publishDomainEvents(order);

        idempotencyFilter.markProcessed(eventId, "InventoryShortageEvent");
        log.info("주문 취소 (재고 부족): orderId={}", orderId);
    }

    private void publishDomainEvents(Order order) {
        for (DomainEvent event : order.getDomainEvents()) {
            try {
                String payload = objectMapper.writeValueAsString(event);
                outboxRepository.save(OutboxEvent.from(event, payload));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("이벤트 직렬화 실패", e);
            }
        }
        order.clearDomainEvents();
    }
}
