package com.pickme.partner.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ContractInfo {
    private final BigDecimal commissionRate;
    private final String settlementCycle;
    private final LocalDate contractStartDate;
    private final LocalDate contractEndDate;

    public ContractInfo(BigDecimal commissionRate, String settlementCycle, LocalDate contractStartDate, LocalDate contractEndDate) {
        if (commissionRate == null || commissionRate.compareTo(BigDecimal.ZERO) < 0 || commissionRate.compareTo(BigDecimal.valueOf(100)) > 0)
            throw new IllegalArgumentException("수수료율은 0~100 사이여야 합니다");
        this.commissionRate = commissionRate;
        this.settlementCycle = settlementCycle;
        this.contractStartDate = contractStartDate;
        this.contractEndDate = contractEndDate;
    }

    public BigDecimal getCommissionRate() { return commissionRate; }
    public String getSettlementCycle() { return settlementCycle; }
    public LocalDate getContractStartDate() { return contractStartDate; }
    public LocalDate getContractEndDate() { return contractEndDate; }
}
