package com.pickme.settlement.domain.model;

import java.time.LocalDate;
import java.util.Objects;

public class SettlementPeriod {

    private final LocalDate startDate;
    private final LocalDate endDate;

    public SettlementPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("정산 기간의 시작일/종료일은 null일 수 없습니다");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("종료일은 시작일 이후여야 합니다");
        }
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static SettlementPeriod daily(LocalDate date) {
        return new SettlementPeriod(date, date);
    }

    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SettlementPeriod that = (SettlementPeriod) o;
        return Objects.equals(startDate, that.startDate) && Objects.equals(endDate, that.endDate);
    }

    @Override
    public int hashCode() { return Objects.hash(startDate, endDate); }
}
