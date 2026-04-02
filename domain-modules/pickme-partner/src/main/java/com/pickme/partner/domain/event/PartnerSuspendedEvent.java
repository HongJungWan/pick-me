package com.pickme.partner.domain.event;

import com.pickme.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class PartnerSuspendedEvent implements DomainEvent {
    private final UUID eventId;
    private final UUID partnerId;
    private final String reason;
    private final Instant occurredAt;

    public PartnerSuspendedEvent(UUID partnerId, String reason) {
        this.eventId = UUID.randomUUID();
        this.partnerId = partnerId;
        this.reason = reason;
        this.occurredAt = Instant.now();
    }

    @Override public UUID getEventId() { return eventId; }
    @Override public String getEventType() { return "PartnerSuspendedEvent"; }
    @Override public String getAggregateType() { return "partner"; }
    @Override public String getAggregateId() { return partnerId.toString(); }
    @Override public Instant getOccurredAt() { return occurredAt; }
    public UUID getPartnerId() { return partnerId; }
    public String getReason() { return reason; }
}
