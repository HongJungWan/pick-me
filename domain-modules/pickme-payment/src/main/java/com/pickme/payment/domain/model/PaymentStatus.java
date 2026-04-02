package com.pickme.payment.domain.model;

import java.util.Map;
import java.util.Set;

public enum PaymentStatus {

    REQUESTED,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUND_REQUESTED,
    REFUNDED;

    private static final Map<PaymentStatus, Set<PaymentStatus>> TRANSITIONS = Map.of(
            REQUESTED, Set.of(PROCESSING),
            PROCESSING, Set.of(COMPLETED, FAILED),
            COMPLETED, Set.of(REFUND_REQUESTED),
            FAILED, Set.of(),
            REFUND_REQUESTED, Set.of(REFUNDED),
            REFUNDED, Set.of()
    );

    public boolean canTransitionTo(PaymentStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
