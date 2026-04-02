package com.pickme.common.outbox;

import com.pickme.common.event.DomainEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false, length = 100)
    private String aggregateType;

    @Column(nullable = false, length = 100)
    private String aggregateId;

    @Column(nullable = false, length = 200)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean published;

    private Instant publishedAt;

    @Column(nullable = false)
    private int retryCount;

    private OutboxEvent(UUID eventId, String aggregateType, String aggregateId,
                        String eventType, String payload) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = Instant.now();
        this.published = false;
        this.retryCount = 0;
    }

    public static OutboxEvent from(DomainEvent event, String serializedPayload) {
        return new OutboxEvent(
                event.getEventId(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getEventType(),
                serializedPayload
        );
    }

    public void markPublished() {
        this.published = true;
        this.publishedAt = Instant.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
