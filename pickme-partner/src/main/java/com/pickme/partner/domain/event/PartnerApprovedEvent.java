package com.pickme.partner.domain.event;

import com.pickme.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class PartnerApprovedEvent implements DomainEvent {
    private final UUID eventId;
    private final UUID partnerId;
    private final String companyName;
    private final Instant occurredAt;

    public PartnerApprovedEvent(UUID partnerId, String companyName) {
        this.eventId = UUID.randomUUID(); this.partnerId = partnerId;
        this.companyName = companyName; this.occurredAt = Instant.now();
    }
    @Override public UUID getEventId() { return eventId; }
    @Override public String getEventType() { return "PartnerApprovedEvent"; }
    @Override public String getAggregateType() { return "partner"; }
    @Override public String getAggregateId() { return partnerId.toString(); }
    @Override public Instant getOccurredAt() { return occurredAt; }
    public UUID getPartnerId() { return partnerId; }
    public String getCompanyName() { return companyName; }
}
