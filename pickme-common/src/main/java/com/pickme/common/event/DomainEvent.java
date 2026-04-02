package com.pickme.common.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {

    UUID getEventId();

    String getEventType();

    String getAggregateType();

    String getAggregateId();

    Instant getOccurredAt();
}
