package com.pickme.notification.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Notification {

    private final NotificationId notificationId;
    private final UUID recipientId;
    private final NotificationChannel channel;
    private final String templateCode;
    private final String subject;
    private final String content;
    private SendStatus sendStatus;
    private Instant sentAt;

    private Notification(NotificationId notificationId, UUID recipientId, NotificationChannel channel,
                         String templateCode, String subject, String content, SendStatus sendStatus, Instant sentAt) {
        this.notificationId = notificationId;
        this.recipientId = recipientId;
        this.channel = channel;
        this.templateCode = templateCode;
        this.subject = subject;
        this.content = content;
        this.sendStatus = sendStatus;
        this.sentAt = sentAt;
    }

    public static Notification create(UUID recipientId, NotificationChannel channel,
                                      String templateCode, String subject, String content) {
        return new Notification(
                NotificationId.generate(), recipientId, channel,
                templateCode, subject, content, SendStatus.PENDING, null
        );
    }

    public static Notification reconstitute(NotificationId id, UUID recipientId, NotificationChannel channel,
                                            String templateCode, String subject, String content,
                                            SendStatus sendStatus, Instant sentAt) {
        return new Notification(id, recipientId, channel, templateCode, subject, content, sendStatus, sentAt);
    }

    public void markSent() {
        this.sendStatus = SendStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void markFailed() {
        this.sendStatus = SendStatus.FAILED;
    }

    public NotificationId getNotificationId() { return notificationId; }
    public UUID getRecipientId() { return recipientId; }
    public NotificationChannel getChannel() { return channel; }
    public String getTemplateCode() { return templateCode; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }
    public SendStatus getSendStatus() { return sendStatus; }
    public Instant getSentAt() { return sentAt; }
}
