package com.pickme.order.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID ordererId,
        @NotEmpty List<OrderLineRequest> orderLines,
        @NotNull ShippingInfoRequest shippingInfo
) {
    public record OrderLineRequest(
            @NotNull UUID productId,
            @NotBlank String productName,
            @Min(1) int quantity,
            @Min(0) long unitPrice
    ) {}

    public record ShippingInfoRequest(
            @NotBlank String receiverName,
            @NotBlank String phone,
            @NotBlank String zipCode,
            @NotBlank String roadAddress,
            String addressDetail
    ) {}
}
