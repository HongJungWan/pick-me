package com.pickme.inventory.domain.model;

import java.util.Objects;
import java.util.UUID;

public class StockId {

    private final UUID id;

    public StockId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("재고 ID는 null일 수 없습니다");
        }
        this.id = id;
    }

    public static StockId generate() {
        return new StockId(UUID.randomUUID());
    }

    public static StockId of(UUID id) {
        return new StockId(id);
    }

    public UUID getValue() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockId stockId = (StockId) o;
        return Objects.equals(id, stockId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
