package com.pickme.order.domain.model;

import java.util.Objects;

public class ShippingInfo {

    private final String receiverName;
    private final String phone;
    private final Address address;

    public ShippingInfo(String receiverName, String phone, Address address) {
        if (receiverName == null || receiverName.isBlank()) throw new IllegalArgumentException("수령인명은 비어있을 수 없습니다");
        if (phone == null || phone.isBlank()) throw new IllegalArgumentException("연락처는 비어있을 수 없습니다");
        if (address == null) throw new IllegalArgumentException("주소는 null일 수 없습니다");
        this.receiverName = receiverName.strip();
        this.phone = phone.strip();
        this.address = address;
    }

    public String getReceiverName() { return receiverName; }
    public String getPhone() { return phone; }
    public Address getAddress() { return address; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShippingInfo s = (ShippingInfo) o;
        return Objects.equals(receiverName, s.receiverName) && Objects.equals(address, s.address);
    }
    @Override public int hashCode() { return Objects.hash(receiverName, address); }
}
