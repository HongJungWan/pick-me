package com.pickme.notification.domain.repository;

import com.pickme.notification.domain.model.Notification;
import com.pickme.notification.domain.model.NotificationId;

import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(NotificationId id);
}
