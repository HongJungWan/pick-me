package com.pickme.orchestration.dto;

import java.util.List;
import java.util.UUID;

public record RefundRequest(
        UUID orderId,
        UUID ordererId,
        String reason,
        long refundAmount,
        List<OrderLineItem> orderLines
) {
}
