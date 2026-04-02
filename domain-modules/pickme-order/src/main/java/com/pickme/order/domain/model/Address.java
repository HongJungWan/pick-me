package com.pickme.order.domain.model;

import java.util.Objects;

public class Address {

    private final String zipCode;
    private final String roadAddress;
    private final String detail;

    public Address(String zipCode, String roadAddress, String detail) {
        if (zipCode == null || zipCode.isBlank()) throw new IllegalArgumentException("우편번호는 비어있을 수 없습니다");
        if (roadAddress == null || roadAddress.isBlank()) throw new IllegalArgumentException("도로명주소는 비어있을 수 없습니다");
        this.zipCode = zipCode.strip();
        this.roadAddress = roadAddress.strip();
        this.detail = detail != null ? detail.strip() : "";
    }

    public String getZipCode() { return zipCode; }
    public String getRoadAddress() { return roadAddress; }
    public String getDetail() { return detail; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address a = (Address) o;
        return Objects.equals(zipCode, a.zipCode) && Objects.equals(roadAddress, a.roadAddress);
    }
    @Override public int hashCode() { return Objects.hash(zipCode, roadAddress); }
}
