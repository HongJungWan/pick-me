package com.pickme.partner.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "partners", schema = "partner_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartnerJpaEntity {
    @Id private UUID id;
    @Column(nullable = false) private String registrationNumber;
    @Column(nullable = false) private String companyName;
    private String representativeName;
    @Column(nullable = false) private BigDecimal commissionRate;
    private String settlementCycle;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15) private StatusJpa status;
    @Column(nullable = false) private Instant createdAt;

    public PartnerJpaEntity(UUID id, String registrationNumber, String companyName, String representativeName,
                            BigDecimal commissionRate, String settlementCycle, LocalDate contractStartDate,
                            LocalDate contractEndDate, StatusJpa status) {
        this.id = id; this.registrationNumber = registrationNumber; this.companyName = companyName;
        this.representativeName = representativeName; this.commissionRate = commissionRate;
        this.settlementCycle = settlementCycle; this.contractStartDate = contractStartDate;
        this.contractEndDate = contractEndDate; this.status = status; this.createdAt = Instant.now();
    }

    public void updateStatus(StatusJpa status) { this.status = status; }

    public enum StatusJpa { PENDING, APPROVED, SUSPENDED, TERMINATED }
}
