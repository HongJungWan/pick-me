package com.pickme.notification.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface JpaNotificationRepository extends JpaRepository<NotificationJpaEntity, UUID> {
}
