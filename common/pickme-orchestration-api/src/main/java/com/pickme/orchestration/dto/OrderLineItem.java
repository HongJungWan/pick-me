package com.pickme.orchestration.dto;

import java.util.UUID;

public record OrderLineItem(
        UUID productId,
        String productName,
        int quantity,
        long unitPrice
) {
}
