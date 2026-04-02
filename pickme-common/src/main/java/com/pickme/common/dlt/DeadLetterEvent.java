package com.pickme.common.dlt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "dead_letter_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeadLetterEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false, length = 200)
    private String eventType;

    @Column(nullable = false, length = 100)
    private String originalTopic;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private DltStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant retriedAt;

    public DeadLetterEvent(UUID eventId, String eventType, String originalTopic,
                           String payload, String errorMessage) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.originalTopic = originalTopic;
        this.payload = payload;
        this.errorMessage = errorMessage;
        this.status = DltStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = Instant.now();
    }

    public void markRetried() {
        this.retryCount++;
        this.retriedAt = Instant.now();
        this.status = DltStatus.RETRIED;
    }

    public void markFailed() {
        this.status = DltStatus.FAILED;
    }

    public enum DltStatus { PENDING, RETRIED, FAILED }
}
