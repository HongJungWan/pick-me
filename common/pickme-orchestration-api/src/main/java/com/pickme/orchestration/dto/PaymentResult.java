package com.pickme.orchestration.dto;

import java.util.UUID;

public record PaymentResult(
        boolean success,
        UUID paymentId,
        String failureReason
) {

    public static PaymentResult success(UUID paymentId) {
        return new PaymentResult(true, paymentId, null);
    }

    public static PaymentResult failure(String reason) {
        return new PaymentResult(false, null, reason);
    }
}
