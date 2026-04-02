package com.pickme.order.domain.event;

import com.pickme.common.event.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OrderPlacedEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID orderId;
    private final UUID ordererId;
    private final List<OrderLinePayload> orderLines;
    private final long totalAmount;
    private final Instant occurredAt;

    public OrderPlacedEvent(UUID orderId, UUID ordererId, List<OrderLinePayload> orderLines, long totalAmount) {
        this.eventId = UUID.randomUUID();
        this.orderId = orderId;
        this.ordererId = ordererId;
        this.orderLines = orderLines;
        this.totalAmount = totalAmount;
        this.occurredAt = Instant.now();
    }

    @Override public UUID getEventId() { return eventId; }
    @Override public String getEventType() { return "OrderPlacedEvent"; }
    @Override public String getAggregateType() { return "order"; }
    @Override public String getAggregateId() { return orderId.toString(); }
    @Override public Instant getOccurredAt() { return occurredAt; }

    public UUID getOrderId() { return orderId; }
    public UUID getOrdererId() { return ordererId; }
    public List<OrderLinePayload> getOrderLines() { return orderLines; }
    public long getTotalAmount() { return totalAmount; }

    public record OrderLinePayload(UUID productId, String productName, int quantity, long unitPrice, long lineTotal) {}
}
