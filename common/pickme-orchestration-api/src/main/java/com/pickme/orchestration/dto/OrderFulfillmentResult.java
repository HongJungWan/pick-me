package com.pickme.orchestration.dto;

import java.util.UUID;

public record OrderFulfillmentResult(
        boolean success,
        UUID orderId,
        UUID paymentId,
        String failureType,
        String failureReason
) {

    public static OrderFulfillmentResult success(UUID orderId, UUID paymentId) {
        return new OrderFulfillmentResult(true, orderId, paymentId, null, null);
    }

    public static OrderFulfillmentResult failed(UUID orderId, String failureType, String reason) {
        return new OrderFulfillmentResult(false, orderId, null, failureType, reason);
    }
}
