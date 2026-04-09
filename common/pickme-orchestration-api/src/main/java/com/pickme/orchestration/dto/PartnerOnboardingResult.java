package com.pickme.orchestration.dto;

import java.util.UUID;

public record PartnerOnboardingResult(
        boolean success,
        UUID partnerId,
        String status,
        String failureReason
) {

    public static PartnerOnboardingResult approved(UUID partnerId) {
        return new PartnerOnboardingResult(true, partnerId, "APPROVED", null);
    }

    public static PartnerOnboardingResult rejected(UUID partnerId, String reason) {
        return new PartnerOnboardingResult(false, partnerId, "REJECTED", reason);
    }

    public static PartnerOnboardingResult expired(UUID partnerId) {
        return new PartnerOnboardingResult(false, partnerId, "EXPIRED", "승인 대기 기간 만료 (7일)");
    }
}
