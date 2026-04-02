package com.pickme.order.domain.model;

import java.util.Set;

public enum OrderStatus {

    PLACED,
    PAYMENT_PENDING,
    PAID,
    PREPARING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUND_REQUESTED,
    REFUNDED;

    private static final java.util.Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = java.util.Map.of(
            PLACED, Set.of(PAYMENT_PENDING, CANCELLED),
            PAYMENT_PENDING, Set.of(PAID, CANCELLED),
            PAID, Set.of(PREPARING, REFUND_REQUESTED),
            PREPARING, Set.of(SHIPPED, REFUND_REQUESTED),
            SHIPPED, Set.of(DELIVERED),
            DELIVERED, Set.of(),
            CANCELLED, Set.of(),
            REFUND_REQUESTED, Set.of(REFUNDED),
            REFUNDED, Set.of()
    );

    public boolean canTransitionTo(OrderStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
