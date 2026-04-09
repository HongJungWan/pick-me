package com.pickme.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PartnerOnboardingRequest(
        String registrationNumber,
        String companyName,
        String representativeName,
        BigDecimal commissionRate,
        String settlementCycle,
        LocalDate contractStartDate,
        LocalDate contractEndDate
) {
}
