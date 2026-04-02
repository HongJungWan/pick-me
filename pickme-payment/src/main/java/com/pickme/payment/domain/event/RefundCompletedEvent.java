package com.pickme.payment.domain.event;

import com.pickme.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class RefundCompletedEvent implements DomainEvent {
    private final UUID eventId;
    private final UUID paymentId;
    private final UUID orderId;
    private final long refundAmount;
    private final Instant occurredAt;

    public RefundCompletedEvent(UUID paymentId, UUID orderId, long refundAmount) {
        this.eventId = UUID.randomUUID();
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.refundAmount = refundAmount;
        this.occurredAt = Instant.now();
    }

    @Override public UUID getEventId() { return eventId; }
    @Override public String getEventType() { return "RefundCompletedEvent"; }
    @Override public String getAggregateType() { return "payment"; }
    @Override public String getAggregateId() { return paymentId.toString(); }
    @Override public Instant getOccurredAt() { return occurredAt; }

    public UUID getPaymentId() { return paymentId; }
    public UUID getOrderId() { return orderId; }
    public long getRefundAmount() { return refundAmount; }
}
