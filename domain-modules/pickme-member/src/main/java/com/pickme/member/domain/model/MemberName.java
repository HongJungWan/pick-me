package com.pickme.member.domain.model;

import java.util.Objects;

public class MemberName {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 50;

    private final String value;

    public MemberName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("이름은 비어있을 수 없습니다");
        }
        String trimmed = value.strip();
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("이름은 %d~%d자여야 합니다. 현재: %d자", MIN_LENGTH, MAX_LENGTH, trimmed.length()));
        }
        this.value = trimmed;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberName that = (MemberName) o;
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
