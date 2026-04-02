package com.pickme.settlement.domain.model;

import java.util.Map;
import java.util.Set;

public enum SettlementStatus {

    CALCULATING,
    CONFIRMED,
    TRANSFER_REQUESTED,
    COMPLETED;

    private static final Map<SettlementStatus, Set<SettlementStatus>> TRANSITIONS = Map.of(
            CALCULATING, Set.of(CONFIRMED),
            CONFIRMED, Set.of(TRANSFER_REQUESTED),
            TRANSFER_REQUESTED, Set.of(COMPLETED),
            COMPLETED, Set.of()
    );

    public boolean canTransitionTo(SettlementStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
