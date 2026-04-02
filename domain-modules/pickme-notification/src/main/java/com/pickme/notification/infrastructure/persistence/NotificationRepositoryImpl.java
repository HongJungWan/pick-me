package com.pickme.notification.infrastructure.persistence;

import com.pickme.notification.domain.model.Notification;
import com.pickme.notification.domain.model.NotificationId;
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
        jpaRepository.save(NotificationMapper.toJpaEntity(n));
        return n;
    }

    @Override
    public Optional<Notification> findById(NotificationId id) {
        return jpaRepository.findById(id.getValue()).map(NotificationMapper::toDomain);
    }
}
