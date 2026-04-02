package com.pickme.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickme.notification.application.NotificationEventHandler;
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
public class NotificationEventConsumer {

    private final NotificationEventHandler handler;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "pickme.order.events", groupId = "notification-consumer")
    public void consumeOrderEvents(String message, Acknowledgment ack) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();
            UUID eventId = UUID.fromString(root.path("eventId").asText());

            if ("OrderPlacedEvent".equals(eventType)) {
                handler.handleOrderPlaced(eventId,
                        UUID.fromString(root.path("ordererId").asText()),
                        UUID.fromString(root.path("orderId").asText()));
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Order 알림 이벤트 처리 실패: {}", message, e);
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "pickme.payment.events", groupId = "notification-consumer")
    public void consumePaymentEvents(String message, Acknowledgment ack) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();
            UUID eventId = UUID.fromString(root.path("eventId").asText());

            if ("PaymentCompletedEvent".equals(eventType)) {
                handler.handlePaymentCompleted(eventId,
                        UUID.fromString(root.path("payerId").asText()),
                        UUID.fromString(root.path("orderId").asText()),
                        root.path("amount").asLong());
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Payment 알림 이벤트 처리 실패: {}", message, e);
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "pickme.member.events", groupId = "notification-consumer")
    public void consumeMemberEvents(String message, Acknowledgment ack) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();
            UUID eventId = UUID.fromString(root.path("eventId").asText());

            if ("MemberRegisteredEvent".equals(eventType)) {
                handler.handleMemberRegistered(eventId,
                        UUID.fromString(root.path("memberId").asText()),
                        root.path("name").asText(),
                        root.path("email").asText());
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Member 알림 이벤트 처리 실패: {}", message, e);
            ack.acknowledge();
        }
    }
}
