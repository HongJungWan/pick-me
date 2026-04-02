package com.pickme.partner.domain.model;

import java.util.Objects;
import java.util.UUID;

public class PartnerId {
    private final UUID id;
    public PartnerId(UUID id) { if (id == null) throw new IllegalArgumentException("파트너 ID는 null일 수 없습니다"); this.id = id; }
    public static PartnerId generate() { return new PartnerId(UUID.randomUUID()); }
    public static PartnerId of(UUID id) { return new PartnerId(id); }
    public UUID getValue() { return id; }
    @Override public boolean equals(Object o) { if (this == o) return true; if (o == null || getClass() != o.getClass()) return false; return Objects.equals(id, ((PartnerId) o).id); }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() { return id.toString(); }
}
