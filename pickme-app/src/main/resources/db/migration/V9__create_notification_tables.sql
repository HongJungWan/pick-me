-- V9: 알림 도메인 테이블

CREATE TABLE notification_schema.notifications (
    id              UUID PRIMARY KEY,
    recipient_id    UUID NOT NULL,
    channel         VARCHAR(20) NOT NULL,
    template_code   VARCHAR(50) NOT NULL,
    subject         VARCHAR(200) NOT NULL,
    content         TEXT NOT NULL,
    send_status     VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    sent_at         TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_recipient ON notification_schema.notifications (recipient_id);
CREATE INDEX idx_notifications_status ON notification_schema.notifications (send_status);
