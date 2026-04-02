package com.pickme.settlement.application;

import com.pickme.settlement.infrastructure.snapshot.SalesSnapshotEntity;
import com.pickme.settlement.infrastructure.snapshot.SalesSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SalesSnapshotRepository salesSnapshotRepository;

    @Transactional(readOnly = true)
    public List<SalesSnapshotEntity> getDailySettlements(LocalDate date) {
        return salesSnapshotRepository.findAll().stream()
                .filter(s -> s.getAggregateDate().equals(date))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SalesSnapshotEntity> getAllSettlements() {
        return salesSnapshotRepository.findAll();
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional(readOnly = true)
    public void reconciliationBatch() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<SalesSnapshotEntity> snapshots = getDailySettlements(yesterday);

        log.info("=== 정산 Reconciliation 배치 시작: date={} ===", yesterday);
        for (SalesSnapshotEntity snapshot : snapshots) {
            if (!snapshot.isReconciled()) {
                log.error("정산 불일치 감지: partnerId={}", snapshot.getPartnerId());
            }
        }
        log.info("=== 정산 Reconciliation 배치 완료: {}건 검증 ===", snapshots.size());
    }
}
