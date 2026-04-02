package com.pickme.payment.domain.model;

import java.util.Objects;

public class Money {

    private final long amount;

    public Money(long amount) {
        if (amount < 0) throw new IllegalArgumentException("금액은 0 이상이어야 합니다");
        this.amount = amount;
    }

    public static Money of(long amount) { return new Money(amount); }

    public long getAmount() { return amount; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return amount == ((Money) o).amount;
    }
    @Override public int hashCode() { return Objects.hash(amount); }
    @Override public String toString() { return String.valueOf(amount); }
}
