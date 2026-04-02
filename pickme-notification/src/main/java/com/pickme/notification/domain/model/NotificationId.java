package com.pickme.notification.domain.model;

import java.util.Objects;
import java.util.UUID;

public class NotificationId {
    private final UUID id;

    public NotificationId(UUID id) {
        if (id == null) throw new IllegalArgumentException("알림 ID는 null일 수 없습니다");
        this.id = id;
    }
    public static NotificationId generate() { return new NotificationId(UUID.randomUUID()); }
    public static NotificationId of(UUID id) { return new NotificationId(id); }
    public UUID getValue() { return id; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(id, ((NotificationId) o).id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() { return id.toString(); }
}
