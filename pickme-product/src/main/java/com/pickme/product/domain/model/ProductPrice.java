package com.pickme.product.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class ProductPrice {

    private final long basePrice;
    private final long discountedPrice;
    private final BigDecimal discountRate;

    public ProductPrice(long basePrice, long discountedPrice) {
        if (basePrice < 0) {
            throw new IllegalArgumentException("기본가는 0 이상이어야 합니다");
        }
        if (discountedPrice < 0) {
            throw new IllegalArgumentException("할인가는 0 이상이어야 합니다");
        }
        if (discountedPrice > basePrice) {
            throw new IllegalArgumentException("할인가는 기본가를 초과할 수 없습니다");
        }
        this.basePrice = basePrice;
        this.discountedPrice = discountedPrice;
        this.discountRate = basePrice > 0
                ? BigDecimal.valueOf(basePrice - discountedPrice)
                    .divide(BigDecimal.valueOf(basePrice), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
    }

    public static ProductPrice of(long basePrice) {
        return new ProductPrice(basePrice, basePrice);
    }

    public static ProductPrice of(long basePrice, long discountedPrice) {
        return new ProductPrice(basePrice, discountedPrice);
    }

    public long getSellingPrice() {
        return discountedPrice;
    }

    public long getBasePrice() {
        return basePrice;
    }

    public long getDiscountedPrice() {
        return discountedPrice;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductPrice that = (ProductPrice) o;
        return basePrice == that.basePrice && discountedPrice == that.discountedPrice;
    }

    @Override
    public int hashCode() {
        return Objects.hash(basePrice, discountedPrice);
    }

    @Override
    public String toString() {
        return String.format("ProductPrice{base=%d, selling=%d, discount=%.1f%%}", basePrice, discountedPrice, discountRate);
    }
}
