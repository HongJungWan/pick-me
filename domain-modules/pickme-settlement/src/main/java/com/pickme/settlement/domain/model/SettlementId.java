package com.pickme.settlement.domain.model;

import java.util.Objects;
import java.util.UUID;

public class SettlementId {
    private final UUID id;
    public SettlementId(UUID id) { if (id == null) throw new IllegalArgumentException("정산 ID는 null일 수 없습니다"); this.id = id; }
    public static SettlementId generate() { return new SettlementId(UUID.randomUUID()); }
    public static SettlementId of(UUID id) { return new SettlementId(id); }
    public UUID getValue() { return id; }
    @Override public boolean equals(Object o) { if (this == o) return true; if (o == null || getClass() != o.getClass()) return false; return Objects.equals(id, ((SettlementId) o).id); }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() { return id.toString(); }
}
