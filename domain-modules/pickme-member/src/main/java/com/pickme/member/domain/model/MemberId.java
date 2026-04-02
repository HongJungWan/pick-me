package com.pickme.member.domain.model;

import java.util.Objects;
import java.util.UUID;

public class MemberId {

    private final UUID id;

    public MemberId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("회원 ID는 null일 수 없습니다");
        }
        this.id = id;
    }

    public static MemberId generate() {
        return new MemberId(UUID.randomUUID());
    }

    public static MemberId of(UUID id) {
        return new MemberId(id);
    }

    public UUID getValue() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberId memberId = (MemberId) o;
        return Objects.equals(id, memberId.id);
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
