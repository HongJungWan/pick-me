package com.pickme.product.api.request;

import jakarta.validation.constraints.Min;

public record UpdateProductRequest(
        String productName,
        String description,
        @Min(0) Long basePrice,
        @Min(0) Long discountedPrice,
        String categoryCode,
        String categoryName,
        String status
) {}
