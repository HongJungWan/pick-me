package com.pickme.order.domain.model;

import java.util.Objects;
import java.util.UUID;

public class OrderId {

    private final UUID id;

    public OrderId(UUID id) {
        if (id == null) throw new IllegalArgumentException("주문 ID는 null일 수 없습니다");
        this.id = id;
    }

    public static OrderId generate() { return new OrderId(UUID.randomUUID()); }
    public static OrderId of(UUID id) { return new OrderId(id); }
    public UUID getValue() { return id; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(id, ((OrderId) o).id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() { return id.toString(); }
}
