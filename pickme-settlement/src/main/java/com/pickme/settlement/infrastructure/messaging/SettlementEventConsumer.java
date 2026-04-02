package com.pickme.settlement.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickme.settlement.application.SettlementEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class SettlementEventConsumer {

    private final SettlementEventHandler handler;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "pickme.payment.events", groupId = "settlement-consumer")
    public void consumePaymentEvents(String message, Acknowledgment ack) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();
            UUID eventId = UUID.fromString(root.path("eventId").asText());

            switch (eventType) {
                case "PaymentCompletedEvent" -> handler.handlePaymentCompleted(
                        eventId,
                        UUID.fromString(root.path("payerId").asText()),
                        root.path("amount").asLong()
                );
                case "RefundCompletedEvent" -> handler.handleRefundCompleted(
                        eventId,
                        UUID.fromString(root.path("orderId").asText()),
                        root.path("refundAmount").asLong()
                );
                default -> log.debug("무시된 Payment 이벤트: {}", eventType);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Settlement 이벤트 처리 실패: {}", message, e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "pickme.partner.events", groupId = "settlement-consumer")
    public void consumePartnerEvents(String message, Acknowledgment ack) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();
            UUID eventId = UUID.fromString(root.path("eventId").asText());

            if ("PartnerApprovedEvent".equals(eventType)) {
                handler.handlePartnerApproved(
                        eventId,
                        UUID.fromString(root.path("partnerId").asText()),
                        root.path("companyName").asText(),
                        new BigDecimal(root.path("commissionRate").asText("0")),
                        root.path("settlementCycle").asText("")
                );
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Partner 이벤트 처리 실패: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}
