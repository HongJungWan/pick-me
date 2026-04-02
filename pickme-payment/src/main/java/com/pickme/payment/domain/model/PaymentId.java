package com.pickme.payment.domain.model;

import java.util.Objects;
import java.util.UUID;

public class PaymentId {

    private final UUID id;

    public PaymentId(UUID id) {
        if (id == null) throw new IllegalArgumentException("결제 ID는 null일 수 없습니다");
        this.id = id;
    }

    public static PaymentId generate() { return new PaymentId(UUID.randomUUID()); }
    public static PaymentId of(UUID id) { return new PaymentId(id); }
    public UUID getValue() { return id; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(id, ((PaymentId) o).id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() { return id.toString(); }
}
