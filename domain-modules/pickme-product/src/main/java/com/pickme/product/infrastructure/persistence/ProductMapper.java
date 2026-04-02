package com.pickme.product.infrastructure.persistence;

import com.pickme.product.domain.model.Category;
import com.pickme.product.domain.model.Product;
import com.pickme.product.domain.model.ProductId;
import com.pickme.product.domain.model.ProductName;
import com.pickme.product.domain.model.ProductOption;
import com.pickme.product.domain.model.ProductPrice;
import com.pickme.product.domain.model.ProductStatus;

import java.util.List;

public final class ProductMapper {

    private ProductMapper() {}

    public static ProductJpaEntity toJpaEntity(Product product) {
        List<ProductOptionJpaEntity> options = product.getOptions().stream()
                .map(o -> new ProductOptionJpaEntity(o.getOptionName(), o.getOptionValue(), o.getAdditionalPrice()))
                .toList();

        return new ProductJpaEntity(
                product.getProductId().getValue(),
                product.getPartnerId(),
                product.getProductName().getValue(),
                product.getDescription(),
                product.getPrice().getBasePrice(),
                product.getPrice().getDiscountedPrice(),
                product.getCategory().getCode(),
                product.getCategory().getName(),
                ProductJpaEntity.ProductStatusJpa.valueOf(product.getStatus().name()),
                new java.util.ArrayList<>(options)
        );
    }

    public static Product toDomain(ProductJpaEntity entity) {
        List<ProductOption> options = entity.getOptions().stream()
                .map(o -> new ProductOption(o.getOptionName(), o.getOptionValue(), o.getAdditionalPrice()))
                .toList();

        return Product.reconstitute(
                ProductId.of(entity.getId()),
                entity.getPartnerId(),
                new ProductName(entity.getProductName()),
                entity.getDescription(),
                ProductPrice.of(entity.getBasePrice(), entity.getDiscountedPrice()),
                new Category(entity.getCategoryCode(), entity.getCategoryName()),
                ProductStatus.valueOf(entity.getStatus().name()),
                options
        );
    }
}
