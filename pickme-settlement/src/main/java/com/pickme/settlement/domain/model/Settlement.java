package com.pickme.settlement.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public class Settlement {

    private final SettlementId settlementId;
    private final UUID partnerId;
    private final SettlementPeriod period;
    private long totalSalesAmount;
    private long totalRefundAmount;
    private BigDecimal commissionRate;
    private long commissionAmount;
    private long netSettlementAmount;
    private int totalOrders;
    private SettlementStatus status;

    private Settlement(SettlementId settlementId, UUID partnerId, SettlementPeriod period,
                       long totalSalesAmount, long totalRefundAmount, BigDecimal commissionRate,
                       int totalOrders, SettlementStatus status) {
        this.settlementId = settlementId;
        this.partnerId = partnerId;
        this.period = period;
        this.totalSalesAmount = totalSalesAmount;
        this.totalRefundAmount = totalRefundAmount;
        this.commissionRate = commissionRate;
        this.totalOrders = totalOrders;
        this.status = status;
        recalculate();
    }

    public static Settlement create(UUID partnerId, SettlementPeriod period, BigDecimal commissionRate) {
        return new Settlement(
                SettlementId.generate(), partnerId, period,
                0, 0, commissionRate, 0, SettlementStatus.CALCULATING
        );
    }

    public static Settlement reconstitute(SettlementId id, UUID partnerId, SettlementPeriod period,
                                          long sales, long refunds, BigDecimal commissionRate,
                                          int totalOrders, SettlementStatus status) {
        return new Settlement(id, partnerId, period, sales, refunds, commissionRate, totalOrders, status);
    }

    public void recordSale(long amount) {
        if (this.status != SettlementStatus.CALCULATING) {
            throw new IllegalStateException("정산 집계 중 상태에서만 매출을 기록할 수 있습니다");
        }
        this.totalSalesAmount += amount;
        this.totalOrders++;
        recalculate();
    }

    public void recordRefund(long amount) {
        if (this.status != SettlementStatus.CALCULATING) {
            throw new IllegalStateException("정산 집계 중 상태에서만 환불을 기록할 수 있습니다");
        }
        this.totalRefundAmount += amount;
        recalculate();
    }

    public void confirm() {
        changeStatus(SettlementStatus.CONFIRMED);
    }

    public void requestTransfer() {
        changeStatus(SettlementStatus.TRANSFER_REQUESTED);
    }

    public void complete() {
        changeStatus(SettlementStatus.COMPLETED);
    }

    private void changeStatus(SettlementStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("정산 상태 전이 불가: %s → %s", this.status, newStatus));
        }
        this.status = newStatus;
    }

    private void recalculate() {
        long netSales = this.totalSalesAmount - this.totalRefundAmount;
        this.commissionAmount = BigDecimal.valueOf(netSales)
                .multiply(this.commissionRate)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .longValue();
        this.netSettlementAmount = netSales - this.commissionAmount;
    }

    public SettlementId getSettlementId() { return settlementId; }
    public UUID getPartnerId() { return partnerId; }
    public SettlementPeriod getPeriod() { return period; }
    public long getTotalSalesAmount() { return totalSalesAmount; }
    public long getTotalRefundAmount() { return totalRefundAmount; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public long getCommissionAmount() { return commissionAmount; }
    public long getNetSettlementAmount() { return netSettlementAmount; }
    public int getTotalOrders() { return totalOrders; }
    public SettlementStatus getStatus() { return status; }
}
