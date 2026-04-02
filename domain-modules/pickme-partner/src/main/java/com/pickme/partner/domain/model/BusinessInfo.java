package com.pickme.partner.domain.model;

import java.util.Objects;

public class BusinessInfo {
    private final String registrationNumber;
    private final String companyName;
    private final String representativeName;

    public BusinessInfo(String registrationNumber, String companyName, String representativeName) {
        if (registrationNumber == null || registrationNumber.isBlank()) throw new IllegalArgumentException("사업자등록번호는 비어있을 수 없습니다");
        if (companyName == null || companyName.isBlank()) throw new IllegalArgumentException("상호명은 비어있을 수 없습니다");
        this.registrationNumber = registrationNumber.strip();
        this.companyName = companyName.strip();
        this.representativeName = representativeName != null ? representativeName.strip() : "";
    }

    public String getRegistrationNumber() { return registrationNumber; }
    public String getCompanyName() { return companyName; }
    public String getRepresentativeName() { return representativeName; }

    @Override public boolean equals(Object o) { if (this == o) return true; if (o == null || getClass() != o.getClass()) return false; return Objects.equals(registrationNumber, ((BusinessInfo) o).registrationNumber); }
    @Override public int hashCode() { return Objects.hash(registrationNumber); }
}
