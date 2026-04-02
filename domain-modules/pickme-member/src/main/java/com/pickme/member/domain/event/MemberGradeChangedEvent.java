package com.pickme.member.domain.event;

import com.pickme.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public class MemberGradeChangedEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID memberId;
    private final String oldGrade;
    private final String newGrade;
    private final Instant occurredAt;

    public MemberGradeChangedEvent(UUID memberId, String oldGrade, String newGrade) {
        this.eventId = UUID.randomUUID();
        this.memberId = memberId;
        this.oldGrade = oldGrade;
        this.newGrade = newGrade;
        this.occurredAt = Instant.now();
    }

    @Override public UUID getEventId() { return eventId; }
    @Override public String getEventType() { return "MemberGradeChangedEvent"; }
    @Override public String getAggregateType() { return "member"; }
    @Override public String getAggregateId() { return memberId.toString(); }
    @Override public Instant getOccurredAt() { return occurredAt; }

    public UUID getMemberId() { return memberId; }
    public String getOldGrade() { return oldGrade; }
    public String getNewGrade() { return newGrade; }
}
