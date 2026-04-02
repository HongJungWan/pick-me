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
    public void consumeProductEvents(String message, Acknowledgment ack) {
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
            log.error("Product 이벤트 처리 실패: {}", message, e);
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "pickme.order.events", groupId = "inventory-saga-consumer")
    public void consumeOrderEvents(String message, Acknowledgment ack) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();
            UUID eventId = UUID.fromString(root.path("eventId").asText());

            switch (eventType) {
                case "OrderPlacedEvent" -> {
                    UUID orderId = UUID.fromString(root.path("orderId").asText());
                    JsonNode orderLines = root.path("orderLines");
                    if (orderLines.isArray()) {
                        for (JsonNode line : orderLines) {
                            UUID productId = UUID.fromString(line.path("productId").asText());
                            int quantity = line.path("quantity").asInt();
                            eventHandler.handleOrderPlaced(eventId, orderId, productId, quantity);
                        }
                    }
                }
                case "OrderConfirmedEvent" -> {
                    UUID orderId = UUID.fromString(root.path("orderId").asText());
                    // OrderConfirmedEvent에는 orderLines 정보가 없으므로,
                    // 실제 구현에서는 orderId로 주문 항목을 조회해야 함 (CQRS snapshot 또는 별도 저장)
                    // 현재는 로그만 남김
                    log.info("OrderConfirmedEvent 수신: orderId={} (확정 처리는 orderLine 정보 필요)", orderId);
                }
                case "OrderCancelledEvent" -> {
                    UUID orderId = UUID.fromString(root.path("orderId").asText());
                    log.info("OrderCancelledEvent 수신: orderId={} (복원 처리는 orderLine 정보 필요)", orderId);
                }
                default -> log.debug("무시된 Order 이벤트: {}", eventType);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Order 이벤트 처리 실패: {}", message, e);
            ack.acknowledge();
        }
    }
}
