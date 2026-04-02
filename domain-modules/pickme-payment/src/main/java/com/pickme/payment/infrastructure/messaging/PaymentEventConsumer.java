package com.pickme.payment.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickme.payment.application.PaymentEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class PaymentEventConsumer {

    private final PaymentEventHandler paymentEventHandler;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "pickme.order.events", groupId = "payment-consumer")
    public void consumeOrderEvents(String message, Acknowledgment ack) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();
            UUID eventId = UUID.fromString(root.path("eventId").asText());

            switch (eventType) {
                case "OrderPlacedEvent" -> paymentEventHandler.handleOrderPlaced(
                        eventId,
                        UUID.fromString(root.path("orderId").asText()),
                        UUID.fromString(root.path("ordererId").asText()),
                        root.path("totalAmount").asLong()
                );
                case "OrderRefundRequestedEvent" -> paymentEventHandler.handleOrderRefundRequested(
                        eventId,
                        UUID.fromString(root.path("orderId").asText()),
                        root.path("refundAmount").asLong()
                );
                default -> log.debug("무시된 Order 이벤트: {}", eventType);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Order 이벤트 처리 실패: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}
