package com.pickme.product.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateProductRequest(
        @NotNull UUID partnerId,
        @NotBlank String productName,
        String description,
        @Min(0) long basePrice,
        @Min(0) long discountedPrice,
        @NotBlank String categoryCode,
        @NotBlank String categoryName,
        List<OptionRequest> options
) {
    public record OptionRequest(
            @NotBlank String optionName,
            @NotBlank String optionValue,
            @Min(0) long additionalPrice
    ) {}
}
