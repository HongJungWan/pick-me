package com.pickme.order.application;

import com.pickme.common.event.DomainEventPublisher;
import com.pickme.common.idempotency.IdempotencyFilter;
import com.pickme.orchestration.port.OrderCommandPort;
import com.pickme.order.domain.model.Order;
import com.pickme.order.domain.model.OrderId;
import com.pickme.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * OrderCommandPort 구현체.
 * Temporal Activity에서 호출되며, 기존 도메인 로직을 재사용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandAdapter implements OrderCommandPort {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;
    private final IdempotencyFilter idempotencyFilter;

    @Transactional
    @Override
    public void confirmOrder(UUID orderId) {
        UUID idempotencyKey = UUID.nameUUIDFromBytes(
                ("temporal-confirm:" + orderId).getBytes(StandardCharsets.UTF_8));

        if (idempotencyFilter.isDuplicate(idempotencyKey)) {
            log.info("중복 Activity 무시 (confirmOrder): orderId={}", orderId);
            return;
        }

        Order order = orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

        order.completePayment();
        orderRepository.save(order);
        eventPublisher.publishAll(order);

        idempotencyFilter.markProcessed(idempotencyKey, "TemporalConfirmOrder");
        log.info("[CommandPort] 주문 확정: orderId={}", orderId);
    }

    @Transactional
    @Override
    public void cancelOrder(UUID orderId, String reason) {
        UUID idempotencyKey = UUID.nameUUIDFromBytes(
                ("temporal-cancel:" + orderId).getBytes(StandardCharsets.UTF_8));

        if (idempotencyFilter.isDuplicate(idempotencyKey)) {
            log.info("중복 Activity 무시 (cancelOrder): orderId={}", orderId);
            return;
        }

        Order order = orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

        order.cancel(reason);
        orderRepository.save(order);
        eventPublisher.publishAll(order);

        idempotencyFilter.markProcessed(idempotencyKey, "TemporalCancelOrder");
        log.info("[CommandPort] 주문 취소: orderId={}, reason={}", orderId, reason);
    }
}
