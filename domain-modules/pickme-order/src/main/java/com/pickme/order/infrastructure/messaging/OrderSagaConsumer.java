package com.pickme.order.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickme.order.application.OrderEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Choreography 기반 사가 소비자.
 * Temporal 활성화 시 비활성화된다 — 워크플로우가 사가 오케스트레이션을 대체.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pickme.temporal.enabled", havingValue = "false", matchIfMissing = true)
public class OrderSagaConsumer {

    private final OrderEventHandler orderEventHandler;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "pickme.payment.events", groupId = "order-saga-consumer")
    public void consumePaymentEvents(String message, Acknowledgment ack) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();
            UUID eventId = UUID.fromString(root.path("eventId").asText());
            UUID orderId = UUID.fromString(root.path("orderId").asText());

            switch (eventType) {
                case "PaymentCompletedEvent" -> orderEventHandler.handlePaymentCompleted(eventId, orderId);
                case "PaymentFailedEvent" -> orderEventHandler.handlePaymentFailed(
                        eventId, orderId, root.path("reason").asText(""));
                default -> log.debug("무시된 Payment 이벤트: {}", eventType);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Payment 이벤트 처리 실패: {}", message, e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "pickme.inventory.events", groupId = "order-saga-consumer")
    public void consumeInventoryEvents(String message, Acknowledgment ack) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();
            UUID eventId = UUID.fromString(root.path("eventId").asText());

            if ("InventoryShortageEvent".equals(eventType)) {
                UUID orderId = UUID.fromString(root.path("orderId").asText());
                orderEventHandler.handleInventoryShortage(eventId, orderId);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Inventory 이벤트 처리 실패: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}
