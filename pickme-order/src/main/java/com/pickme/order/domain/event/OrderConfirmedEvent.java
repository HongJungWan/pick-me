package com.pickme.order.domain.event;

import com.pickme.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class OrderConfirmedEvent implements DomainEvent {
    private final UUID eventId;
    private final UUID orderId;
    private final Instant occurredAt;

    public OrderConfirmedEvent(UUID orderId) {
        this.eventId = UUID.randomUUID();
        this.orderId = orderId;
        this.occurredAt = Instant.now();
    }

    @Override public UUID getEventId() { return eventId; }
    @Override public String getEventType() { return "OrderConfirmedEvent"; }
    @Override public String getAggregateType() { return "order"; }
    @Override public String getAggregateId() { return orderId.toString(); }
    @Override public Instant getOccurredAt() { return occurredAt; }
    public UUID getOrderId() { return orderId; }
}
