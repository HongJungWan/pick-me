package com.pickme.orchestration.port;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 정산 도메인에 대한 명령 포트.
 * Temporal Activity가 이 인터페이스를 통해 정산 조회/검증을 요청한다.
 */
public interface SettlementCommandPort {

    List<PartnerSettlementInfo> fetchDailySnapshots(LocalDate date);

    ReconciliationResult reconcilePartner(UUID partnerId, LocalDate date);

    record PartnerSettlementInfo(UUID partnerId, long totalSales, long totalRefunds, boolean reconciled) {}

    record ReconciliationResult(UUID partnerId, boolean success, String discrepancy) {}
}
