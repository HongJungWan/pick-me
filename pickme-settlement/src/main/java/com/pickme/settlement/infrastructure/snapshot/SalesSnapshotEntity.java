package com.pickme.settlement.infrastructure.snapshot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_sales_aggregate", schema = "settlement_schema")
@IdClass(SalesSnapshotEntity.SalesSnapshotId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesSnapshotEntity {

    @Id
    private LocalDate aggregateDate;

    @Id
    private UUID partnerId;

    @Column(nullable = false)
    private int totalOrders;

    @Column(nullable = false)
    private long totalSales;

    @Column(nullable = false)
    private long totalRefunds;

    @Column(nullable = false)
    private long netSales;

    @Column(nullable = false)
    private Instant updatedAt;

    public SalesSnapshotEntity(LocalDate aggregateDate, UUID partnerId) {
        this.aggregateDate = aggregateDate;
        this.partnerId = partnerId;
        this.totalOrders = 0;
        this.totalSales = 0;
        this.totalRefunds = 0;
        this.netSales = 0;
        this.updatedAt = Instant.now();
    }

    public void addSale(long amount) {
        this.totalOrders++;
        this.totalSales += amount;
        this.netSales = this.totalSales - this.totalRefunds;
        this.updatedAt = Instant.now();
    }

    public void addRefund(long amount) {
        this.totalRefunds += amount;
        this.netSales = this.totalSales - this.totalRefunds;
        this.updatedAt = Instant.now();
    }

    public static class SalesSnapshotId implements Serializable {
        private LocalDate aggregateDate;
        private UUID partnerId;

        public SalesSnapshotId() {}
        public SalesSnapshotId(LocalDate aggregateDate, UUID partnerId) {
            this.aggregateDate = aggregateDate;
            this.partnerId = partnerId;
        }
    }
}
