package com.pickme.common.event;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope(
        UUID eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        int version,
        String traceId,
        String payload
) {

    public static EventEnvelope of(DomainEvent event, String serializedPayload) {
        return new EventEnvelope(
                event.getEventId(),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getOccurredAt(),
                1,
                null,
                serializedPayload
        );
    }

    public static EventEnvelope of(DomainEvent event, String serializedPayload, String traceId) {
        return new EventEnvelope(
                event.getEventId(),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getOccurredAt(),
                1,
                traceId,
                serializedPayload
        );
    }
}
