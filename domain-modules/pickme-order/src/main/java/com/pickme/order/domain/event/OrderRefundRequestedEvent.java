package com.pickme.order.domain.event;

import com.pickme.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class OrderRefundRequestedEvent implements DomainEvent {
    private final UUID eventId;
    private final UUID orderId;
    private final long refundAmount;
    private final String reason;
    private final Instant occurredAt;

    public OrderRefundRequestedEvent(UUID orderId, long refundAmount, String reason) {
        this.eventId = UUID.randomUUID();
        this.orderId = orderId;
        this.refundAmount = refundAmount;
        this.reason = reason;
        this.occurredAt = Instant.now();
    }

    @Override public UUID getEventId() { return eventId; }
    @Override public String getEventType() { return "OrderRefundRequestedEvent"; }
    @Override public String getAggregateType() { return "order"; }
    @Override public String getAggregateId() { return orderId.toString(); }
    @Override public Instant getOccurredAt() { return occurredAt; }
    public UUID getOrderId() { return orderId; }
    public long getRefundAmount() { return refundAmount; }
    public String getReason() { return reason; }
}
