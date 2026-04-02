package com.pickme.partner.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePartnerRequest(
        @NotBlank String registrationNumber,
        @NotBlank String companyName,
        String representativeName,
        @NotNull BigDecimal commissionRate,
        String settlementCycle,
        LocalDate contractStartDate,
        LocalDate contractEndDate
) {}
