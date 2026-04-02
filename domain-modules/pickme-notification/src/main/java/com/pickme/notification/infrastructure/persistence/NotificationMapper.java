package com.pickme.notification.infrastructure.persistence;

import com.pickme.notification.domain.model.Notification;
import com.pickme.notification.domain.model.NotificationChannel;
import com.pickme.notification.domain.model.NotificationId;
import com.pickme.notification.domain.model.SendStatus;

public final class NotificationMapper {

    private NotificationMapper() {}

    public static NotificationJpaEntity toJpaEntity(Notification n) {
        return new NotificationJpaEntity(
                n.getNotificationId().getValue(), n.getRecipientId(), n.getChannel().name(),
                n.getTemplateCode(), n.getSubject(), n.getContent(),
                n.getSendStatus().name(), n.getSentAt()
        );
    }

    public static Notification toDomain(NotificationJpaEntity e) {
        return Notification.reconstitute(
                NotificationId.of(e.getId()), e.getRecipientId(),
                NotificationChannel.valueOf(e.getChannel()), e.getTemplateCode(),
                e.getSubject(), e.getContent(), SendStatus.valueOf(e.getSendStatus()), e.getSentAt()
        );
    }
}
