package com.pickme.orchestration.dto;

import java.util.UUID;

public record ReserveResult(
        boolean success,
        UUID orderId,
        String failureReason
) {

    public static ReserveResult success(UUID orderId) {
        return new ReserveResult(true, orderId, null);
    }

    public static ReserveResult failure(UUID orderId, String reason) {
        return new ReserveResult(false, orderId, reason);
    }
}
