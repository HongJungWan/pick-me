package com.pickme.member.domain.event;

import com.pickme.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public class MemberRegisteredEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID memberId;
    private final String name;
    private final String email;
    private final Instant occurredAt;

    public MemberRegisteredEvent(UUID memberId, String name, String email) {
        this.eventId = UUID.randomUUID();
        this.memberId = memberId;
        this.name = name;
        this.email = email;
        this.occurredAt = Instant.now();
    }

    @Override public UUID getEventId() { return eventId; }
    @Override public String getEventType() { return "MemberRegisteredEvent"; }
    @Override public String getAggregateType() { return "member"; }
    @Override public String getAggregateId() { return memberId.toString(); }
    @Override public Instant getOccurredAt() { return occurredAt; }

    public UUID getMemberId() { return memberId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
}
