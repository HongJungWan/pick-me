package com.pickme.payment.domain.event;

import com.pickme.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class PaymentFailedEvent implements DomainEvent {
    private final UUID eventId;
    private final UUID paymentId;
    private final UUID orderId;
    private final String reason;
    private final Instant occurredAt;

    public PaymentFailedEvent(UUID paymentId, UUID orderId, String reason) {
        this.eventId = UUID.randomUUID();
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.reason = reason;
        this.occurredAt = Instant.now();
    }

    @Override public UUID getEventId() { return eventId; }
    @Override public String getEventType() { return "PaymentFailedEvent"; }
    @Override public String getAggregateType() { return "payment"; }
    @Override public String getAggregateId() { return paymentId.toString(); }
    @Override public Instant getOccurredAt() { return occurredAt; }

    public UUID getPaymentId() { return paymentId; }
    public UUID getOrderId() { return orderId; }
    public String getReason() { return reason; }
}
