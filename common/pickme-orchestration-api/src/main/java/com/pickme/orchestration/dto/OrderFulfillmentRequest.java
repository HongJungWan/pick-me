package com.pickme.orchestration.dto;

import java.util.List;
import java.util.UUID;

public record OrderFulfillmentRequest(
        UUID orderId,
        UUID ordererId,
        List<OrderLineItem> orderLines,
        long totalAmount,
        String paymentMethod
) {
}
