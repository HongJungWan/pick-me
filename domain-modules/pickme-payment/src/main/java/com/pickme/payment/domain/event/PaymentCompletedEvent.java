package com.pickme.payment.domain.event;

import com.pickme.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class PaymentCompletedEvent implements DomainEvent {
    private final UUID eventId;
    private final UUID paymentId;
    private final UUID orderId;
    private final UUID payerId;
    private final long amount;
    private final String paymentMethod;
    private final String pgTransactionId;
    private final Instant paidAt;
    private final Instant occurredAt;

    public PaymentCompletedEvent(UUID paymentId, UUID orderId, UUID payerId, long amount,
                                 String paymentMethod, String pgTransactionId) {
        this.eventId = UUID.randomUUID();
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.payerId = payerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.pgTransactionId = pgTransactionId;
        this.paidAt = Instant.now();
        this.occurredAt = Instant.now();
    }

    @Override public UUID getEventId() { return eventId; }
    @Override public String getEventType() { return "PaymentCompletedEvent"; }
    @Override public String getAggregateType() { return "payment"; }
    @Override public String getAggregateId() { return paymentId.toString(); }
    @Override public Instant getOccurredAt() { return occurredAt; }

    public UUID getPaymentId() { return paymentId; }
    public UUID getOrderId() { return orderId; }
    public UUID getPayerId() { return payerId; }
    public long getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPgTransactionId() { return pgTransactionId; }
    public Instant getPaidAt() { return paidAt; }
}
