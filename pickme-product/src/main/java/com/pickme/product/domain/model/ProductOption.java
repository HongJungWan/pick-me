package com.pickme.product.domain.model;

import java.util.Objects;

public class ProductOption {

    private final String optionName;
    private final String optionValue;
    private final long additionalPrice;

    public ProductOption(String optionName, String optionValue, long additionalPrice) {
        if (optionName == null || optionName.isBlank()) {
            throw new IllegalArgumentException("옵션명은 비어있을 수 없습니다");
        }
        if (optionValue == null || optionValue.isBlank()) {
            throw new IllegalArgumentException("옵션값은 비어있을 수 없습니다");
        }
        if (additionalPrice < 0) {
            throw new IllegalArgumentException("추가금액은 0 이상이어야 합니다");
        }
        this.optionName = optionName.strip();
        this.optionValue = optionValue.strip();
        this.additionalPrice = additionalPrice;
    }

    public String getOptionName() {
        return optionName;
    }

    public String getOptionValue() {
        return optionValue;
    }

    public long getAdditionalPrice() {
        return additionalPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductOption that = (ProductOption) o;
        return Objects.equals(optionName, that.optionName) && Objects.equals(optionValue, that.optionValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(optionName, optionValue);
    }
}
