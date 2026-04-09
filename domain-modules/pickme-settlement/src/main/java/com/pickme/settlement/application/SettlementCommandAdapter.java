package com.pickme.settlement.application;

import com.pickme.orchestration.port.SettlementCommandPort;
import com.pickme.settlement.infrastructure.snapshot.SalesSnapshotEntity;
import com.pickme.settlement.infrastructure.snapshot.SalesSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementCommandAdapter implements SettlementCommandPort {

    private final SalesSnapshotRepository salesSnapshotRepository;

    @Transactional(readOnly = true)
    @Override
    public List<PartnerSettlementInfo> fetchDailySnapshots(LocalDate date) {
        log.info("[CommandPort] 일일 정산 스냅샷 조회: date={}", date);
        return salesSnapshotRepository.findAll().stream()
                .filter(s -> s.getAggregateDate().equals(date))
                .map(s -> new PartnerSettlementInfo(
                        s.getPartnerId(), s.getTotalSales(), s.getTotalRefunds(), s.isReconciled()))
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public ReconciliationResult reconcilePartner(UUID partnerId, LocalDate date) {
        log.info("[CommandPort] 파트너 정산 검증: partnerId={}, date={}", partnerId, date);
        List<SalesSnapshotEntity> snapshots = salesSnapshotRepository.findAll().stream()
                .filter(s -> s.getPartnerId().equals(partnerId) && s.getAggregateDate().equals(date))
                .toList();

        if (snapshots.isEmpty()) {
            return new ReconciliationResult(partnerId, true, null);
        }

        for (SalesSnapshotEntity snapshot : snapshots) {
            if (!snapshot.isReconciled()) {
                String discrepancy = String.format("partnerId=%s, sales=%d, refunds=%d",
                        partnerId, snapshot.getTotalSales(), snapshot.getTotalRefunds());
                return new ReconciliationResult(partnerId, false, discrepancy);
            }
        }
        return new ReconciliationResult(partnerId, true, null);
    }
}
