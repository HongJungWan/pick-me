package com.pickme.settlement.infrastructure.snapshot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "partner_snapshot", schema = "settlement_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartnerSnapshotEntity {

    @Id
    private UUID partnerId;

    @Column(nullable = false, length = 100)
    private String companyName;

    @Column(nullable = false)
    private BigDecimal commissionRate;

    @Column(length = 20)
    private String settlementCycle;

    @Column(nullable = false, length = 15)
    private String status;

    @Column(nullable = false)
    private Instant updatedAt;

    public PartnerSnapshotEntity(UUID partnerId, String companyName, BigDecimal commissionRate,
                                 String settlementCycle, String status) {
        this.partnerId = partnerId;
        this.companyName = companyName;
        this.commissionRate = commissionRate;
        this.settlementCycle = settlementCycle;
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void update(String companyName, BigDecimal commissionRate, String status) {
        this.companyName = companyName;
        this.commissionRate = commissionRate;
        this.status = status;
        this.updatedAt = Instant.now();
    }
}
