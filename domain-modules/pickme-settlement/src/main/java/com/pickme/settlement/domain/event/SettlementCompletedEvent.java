package com.pickme.settlement.domain.event;

import com.pickme.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public class SettlementCompletedEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID settlementId;
    private final UUID partnerId;
    private final long netSettlementAmount;
    private final Instant occurredAt;

    public SettlementCompletedEvent(UUID settlementId, UUID partnerId, long netSettlementAmount) {
        this.eventId = UUID.randomUUID();
        this.settlementId = settlementId;
        this.partnerId = partnerId;
        this.netSettlementAmount = netSettlementAmount;
        this.occurredAt = Instant.now();
    }

    @Override public UUID getEventId() { return eventId; }
    @Override public String getEventType() { return "SettlementCompletedEvent"; }
    @Override public String getAggregateType() { return "settlement"; }
    @Override public String getAggregateId() { return settlementId.toString(); }
    @Override public Instant getOccurredAt() { return occurredAt; }
    public UUID getSettlementId() { return settlementId; }
    public UUID getPartnerId() { return partnerId; }
    public long getNetSettlementAmount() { return netSettlementAmount; }
}
