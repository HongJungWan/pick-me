package com.pickme.product.domain.model;

public enum ProductStatus {

    DRAFT,
    ON_SALE,
    SOLD_OUT,
    HIDDEN,
    DISCONTINUED;

    public boolean canTransitionTo(ProductStatus target) {
        return switch (this) {
            case DRAFT -> target == ON_SALE;
            case ON_SALE -> target == SOLD_OUT || target == HIDDEN || target == DISCONTINUED;
            case SOLD_OUT -> target == ON_SALE || target == DISCONTINUED;
            case HIDDEN -> target == ON_SALE || target == DISCONTINUED;
            case DISCONTINUED -> false;
        };
    }
}
