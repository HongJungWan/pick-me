package com.pickme.inventory.domain.model;

import java.util.Objects;

public class Quantity {

    private final int value;

    public Quantity(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("수량은 0 이상이어야 합니다. 현재: " + value);
        }
        this.value = value;
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public static Quantity zero() {
        return new Quantity(0);
    }

    public Quantity add(int amount) {
        return new Quantity(this.value + amount);
    }

    public Quantity subtract(int amount) {
        if (this.value < amount) {
            throw new IllegalStateException(
                    String.format("수량 부족: 현재=%d, 요청=%d", this.value, amount));
        }
        return new Quantity(this.value - amount);
    }

    public boolean isGreaterThanOrEqual(int amount) {
        return this.value >= amount;
    }

    public boolean isZero() {
        return this.value == 0;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quantity quantity = (Quantity) o;
        return value == quantity.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
