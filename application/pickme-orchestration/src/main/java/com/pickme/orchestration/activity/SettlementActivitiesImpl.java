package com.pickme.orchestration.activity;

import com.pickme.orchestration.port.SettlementCommandPort;
import com.pickme.orchestration.port.SettlementCommandPort.PartnerSettlementInfo;
import com.pickme.orchestration.port.SettlementCommandPort.ReconciliationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementActivitiesImpl implements SettlementActivities {

    private final SettlementCommandPort settlementCommandPort;

    @Override
    public List<PartnerSettlementInfo> fetchDailySnapshots(LocalDate date) {
        log.info("[Activity] 일일 정산 스냅샷 조회: date={}", date);
        return settlementCommandPort.fetchDailySnapshots(date);
    }

    @Override
    public ReconciliationResult reconcilePartner(UUID partnerId, LocalDate date) {
        log.info("[Activity] 파트너 정산 검증: partnerId={}, date={}", partnerId, date);
        return settlementCommandPort.reconcilePartner(partnerId, date);
    }

    @Override
    public void reportDiscrepancies(LocalDate date, List<String> discrepancies) {
        log.error("[Activity] 정산 불일치 보고: date={}, count={}", date, discrepancies.size());
        discrepancies.forEach(d -> log.error("  - {}", d));
        // TODO: SlackNotifier 연동
    }
}
