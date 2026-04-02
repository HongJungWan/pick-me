package com.pickme.product.domain.model;

import java.util.Objects;
import java.util.UUID;

public class ProductId {

    private final UUID id;

    public ProductId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("상품 ID는 null일 수 없습니다");
        }
        this.id = id;
    }

    public static ProductId generate() {
        return new ProductId(UUID.randomUUID());
    }

    public static ProductId of(UUID id) {
        return new ProductId(id);
    }

    public UUID getValue() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductId productId = (ProductId) o;
        return Objects.equals(id, productId.id);
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
