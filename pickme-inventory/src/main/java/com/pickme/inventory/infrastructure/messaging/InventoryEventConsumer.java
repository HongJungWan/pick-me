package com.pickme.inventory.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickme.inventory.application.InventoryEventHandler;
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
public class InventoryEventConsumer {

    private final InventoryEventHandler eventHandler;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "pickme.product.events", groupId = "inventory-consumer")
    public void consume(String message, Acknowledgment ack) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();

            if ("ProductRegisteredEvent".equals(eventType)) {
                UUID eventId = UUID.fromString(root.path("eventId").asText());
                UUID productId = UUID.fromString(root.path("productId").asText());
                String productName = root.path("productName").asText();

                eventHandler.handleProductRegistered(eventId, productId, productName);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("이벤트 처리 실패: message={}", message, e);
            ack.acknowledge();
        }
    }
}
