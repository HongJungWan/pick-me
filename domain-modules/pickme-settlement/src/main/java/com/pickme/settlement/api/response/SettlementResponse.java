package com.pickme.settlement.api.response;

import com.pickme.settlement.infrastructure.snapshot.SalesSnapshotEntity;

import java.time.LocalDate;
import java.util.UUID;

public record SettlementResponse(
        LocalDate aggregateDate,
        UUID partnerId,
        int totalOrders,
        long totalSales,
        long totalRefunds,
        long netSales
) {
    public static SettlementResponse from(SalesSnapshotEntity s) {
        return new SettlementResponse(
                s.getAggregateDate(), s.getPartnerId(),
                s.getTotalOrders(), s.getTotalSales(), s.getTotalRefunds(), s.getNetSales()
        );
    }
}
