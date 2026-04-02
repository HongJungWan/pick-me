package com.pickme.order.domain.event;

import com.pickme.common.event.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OrderCancelledEvent implements DomainEvent {
    private final UUID eventId;
    private final UUID orderId;
    private final String reason;
    private final List<OrderLinePayload> orderLines;
    private final Instant occurredAt;

    public OrderCancelledEvent(UUID orderId, String reason, List<OrderLinePayload> orderLines) {
        this.eventId = UUID.randomUUID();
        this.orderId = orderId;
        this.reason = reason;
        this.orderLines = orderLines;
        this.occurredAt = Instant.now();
    }

    @Override public UUID getEventId() { return eventId; }
    @Override public String getEventType() { return "OrderCancelledEvent"; }
    @Override public String getAggregateType() { return "order"; }
    @Override public String getAggregateId() { return orderId.toString(); }
    @Override public Instant getOccurredAt() { return occurredAt; }
    public UUID getOrderId() { return orderId; }
    public String getReason() { return reason; }
    public List<OrderLinePayload> getOrderLines() { return orderLines; }

    public record OrderLinePayload(UUID productId, int quantity) {}
}
