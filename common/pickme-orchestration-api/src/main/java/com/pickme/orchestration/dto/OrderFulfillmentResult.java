package com.pickme.orchestration.dto;

import java.util.UUID;

public record OrderFulfillmentResult(
        boolean success,
        UUID orderId,
        UUID paymentId,
        String failureType,
        String failureReason,
        boolean compensationFailed
) {

    public static OrderFulfillmentResult success(UUID orderId, UUID paymentId) {
        return new OrderFulfillmentResult(true, orderId, paymentId, null, null, false);
    }

    public static OrderFulfillmentResult failed(UUID orderId, String failureType, String reason) {
        return new OrderFulfillmentResult(false, orderId, null, failureType, reason, false);
    }

    public static OrderFulfillmentResult failedWithCompensationError(UUID orderId, String failureType, String reason) {
        return new OrderFulfillmentResult(false, orderId, null, failureType, reason, true);
    }
}
