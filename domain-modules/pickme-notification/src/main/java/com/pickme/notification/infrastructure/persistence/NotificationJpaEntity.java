package com.pickme.notification.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications", schema = "notification_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationJpaEntity {

    @Id private UUID id;
    @Column(nullable = false) private UUID recipientId;
    @Column(nullable = false, length = 20) private String channel;
    @Column(nullable = false, length = 50) private String templateCode;
    @Column(nullable = false) private String subject;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(nullable = false, length = 10) private String sendStatus;
    private Instant sentAt;
    @Column(nullable = false) private Instant createdAt;

    public NotificationJpaEntity(UUID id, UUID recipientId, String channel, String templateCode,
                                 String subject, String content, String sendStatus, Instant sentAt) {
        this.id = id; this.recipientId = recipientId; this.channel = channel;
        this.templateCode = templateCode; this.subject = subject; this.content = content;
        this.sendStatus = sendStatus; this.sentAt = sentAt; this.createdAt = Instant.now();
    }

    public void updateStatus(String sendStatus, Instant sentAt) {
        this.sendStatus = sendStatus; this.sentAt = sentAt;
    }
}
