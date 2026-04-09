package com.pickme.orchestration.dto;

import java.util.UUID;

public record RefundResult(
        boolean success,
        UUID orderId,
        String failureReason
) {

    public static RefundResult success(UUID orderId) {
        return new RefundResult(true, orderId, null);
    }

    public static RefundResult failed(UUID orderId, String reason) {
        return new RefundResult(false, orderId, reason);
    }
}
