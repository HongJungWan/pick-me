package com.pickme.member.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

public class PhoneNumber {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^010-\\d{4}-\\d{4}$");

    private final String value;

    public PhoneNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("전화번호는 비어있을 수 없습니다");
        }
        if (!PHONE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("전화번호 형식이 올바르지 않습니다 (010-XXXX-XXXX): " + value);
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhoneNumber that = (PhoneNumber) o;
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
