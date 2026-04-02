package com.pickme.order.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickme.order.application.OrderSnapshotEventHandler;
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
public class OrderSnapshotConsumer {

    private final OrderSnapshotEventHandler snapshotHandler;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "pickme.product.events", groupId = "order-snapshot-consumer")
    public void consumeProductEvents(String message, Acknowledgment ack) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();
            UUID eventId = UUID.fromString(root.path("eventId").asText());

            switch (eventType) {
                case "ProductRegisteredEvent" -> snapshotHandler.handleProductRegistered(
                        eventId,
                        UUID.fromString(root.path("productId").asText()),
                        root.path("productName").asText(),
                        root.path("sellingPrice").asLong()
                );
                case "ProductInfoChangedEvent" -> snapshotHandler.handleProductInfoChanged(
                        eventId,
                        UUID.fromString(root.path("productId").asText()),
                        root.path("productName").asText()
                );
                default -> log.debug("무시된 이벤트: {}", eventType);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Product 이벤트 처리 실패: {}", message, e);
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "pickme.member.events", groupId = "order-snapshot-consumer")
    public void consumeMemberEvents(String message, Acknowledgment ack) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();
            UUID eventId = UUID.fromString(root.path("eventId").asText());

            switch (eventType) {
                case "MemberRegisteredEvent" -> snapshotHandler.handleMemberRegistered(
                        eventId,
                        UUID.fromString(root.path("memberId").asText()),
                        root.path("name").asText()
                );
                case "MemberGradeChangedEvent" -> snapshotHandler.handleMemberGradeChanged(
                        eventId,
                        UUID.fromString(root.path("memberId").asText()),
                        root.path("newGrade").asText()
                );
                default -> log.debug("무시된 이벤트: {}", eventType);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Member 이벤트 처리 실패: {}", message, e);
            ack.acknowledge();
        }
    }
}
