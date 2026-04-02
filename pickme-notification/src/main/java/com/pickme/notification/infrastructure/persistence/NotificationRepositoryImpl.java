package com.pickme.notification.infrastructure.persistence;

import com.pickme.notification.domain.model.Notification;
import com.pickme.notification.domain.model.NotificationChannel;
import com.pickme.notification.domain.model.NotificationId;
import com.pickme.notification.domain.model.SendStatus;
import com.pickme.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private final JpaNotificationRepository jpaRepository;

    @Override
    public Notification save(Notification n) {
        NotificationJpaEntity entity = new NotificationJpaEntity(
                n.getNotificationId().getValue(), n.getRecipientId(), n.getChannel().name(),
                n.getTemplateCode(), n.getSubject(), n.getContent(),
                n.getSendStatus().name(), n.getSentAt()
        );
        jpaRepository.save(entity);
        return n;
    }

    @Override
    public Optional<Notification> findById(NotificationId id) {
        return jpaRepository.findById(id.getValue()).map(e -> Notification.reconstitute(
                NotificationId.of(e.getId()), e.getRecipientId(),
                NotificationChannel.valueOf(e.getChannel()), e.getTemplateCode(),
                e.getSubject(), e.getContent(), SendStatus.valueOf(e.getSendStatus()), e.getSentAt()
        ));
    }
}
