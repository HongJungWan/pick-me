package com.pickme.product.domain.model;

import java.util.Objects;

public class ProductName {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 200;

    private final String value;

    public ProductName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("상품명은 비어있을 수 없습니다");
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("상품명은 %d~%d자여야 합니다. 현재: %d자", MIN_LENGTH, MAX_LENGTH, value.length()));
        }
        this.value = value.strip();
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductName that = (ProductName) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
