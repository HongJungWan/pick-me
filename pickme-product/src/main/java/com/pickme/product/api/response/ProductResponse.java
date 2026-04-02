package com.pickme.product.api.response;

import com.pickme.product.domain.model.Product;

import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID productId,
        UUID partnerId,
        String productName,
        String description,
        long basePrice,
        long sellingPrice,
        String discountRate,
        String categoryCode,
        String categoryName,
        String status,
        List<OptionResponse> options
) {

    public record OptionResponse(
            String optionName,
            String optionValue,
            long additionalPrice
    ) {}

    public static ProductResponse from(Product product) {
        List<OptionResponse> options = product.getOptions().stream()
                .map(o -> new OptionResponse(o.getOptionName(), o.getOptionValue(), o.getAdditionalPrice()))
                .toList();

        return new ProductResponse(
                product.getProductId().getValue(),
                product.getPartnerId(),
                product.getProductName().getValue(),
                product.getDescription(),
                product.getPrice().getBasePrice(),
                product.getPrice().getSellingPrice(),
                product.getPrice().getDiscountRate().toPlainString() + "%",
                product.getCategory().getCode(),
                product.getCategory().getName(),
                product.getStatus().name(),
                options
        );
    }
}
