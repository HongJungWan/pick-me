package com.pickme.inventory.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickme.inventory.application.InventoryEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
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
    private final Environment environment;

    /**
     * 상품 등록 시 Stock 생성 — 사가와 무관, Temporal 활성화 여부와 상관없이 항상 동작.
     */
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
            throw new RuntimeException(e);
        }
    }

    /**
     * 주문 사가 관련 재고 처리 — Temporal 활성화 시 워크플로우 Activity가 대체하므로 스킵.
     */
    @KafkaListener(topics = "pickme.order.events", groupId = "inventory-saga-consumer")
    public void consumeOrderEvents(String message, Acknowledgment ack) {
        try {
            if (Boolean.parseBoolean(environment.getProperty("pickme.temporal.enabled", "false"))) {
                ack.acknowledge();
                return;
            }

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
                    JsonNode confirmedLines = root.path("orderLines");
                    if (confirmedLines.isArray()) {
                        for (JsonNode line : confirmedLines) {
                            UUID productId = UUID.fromString(line.path("productId").asText());
                            int quantity = line.path("quantity").asInt();
                            eventHandler.handleOrderConfirmed(eventId, orderId, productId, quantity);
                        }
                    }
                }
                case "OrderCancelledEvent" -> {
                    UUID orderId = UUID.fromString(root.path("orderId").asText());
                    JsonNode cancelledLines = root.path("orderLines");
                    if (cancelledLines.isArray()) {
                        for (JsonNode line : cancelledLines) {
                            UUID productId = UUID.fromString(line.path("productId").asText());
                            int quantity = line.path("quantity").asInt();
                            eventHandler.handleOrderCancelled(eventId, orderId, productId, quantity);
                        }
                    }
                }
                default -> log.debug("무시된 Order 이벤트: {}", eventType);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Order 이벤트 처리 실패: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}
