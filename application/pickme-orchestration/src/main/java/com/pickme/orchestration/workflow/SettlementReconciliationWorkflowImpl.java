package com.pickme.orchestration.workflow;

import com.pickme.orchestration.activity.SettlementActivities;
import com.pickme.orchestration.port.SettlementCommandPort.PartnerSettlementInfo;
import com.pickme.orchestration.port.SettlementCommandPort.ReconciliationResult;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SettlementReconciliationWorkflowImpl implements SettlementReconciliationWorkflow {

    private final SettlementActivities activities = Workflow.newActivityStub(
            SettlementActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(5))
                            .build())
                    .build()
    );

    private String status = "STARTED";

    @Override
    public ReconciliationReport execute(LocalDate date) {
        // Step 1: 일일 스냅샷 조회
        status = "FETCHING_SNAPSHOTS";
        List<PartnerSettlementInfo> snapshots = activities.fetchDailySnapshots(date);

        if (snapshots.isEmpty()) {
            status = "COMPLETED";
            return new ReconciliationReport(date, 0, 0, List.of());
        }

        // Step 2: 파트너별 검증
        status = "RECONCILING";
        List<String> discrepancies = new ArrayList<>();
        int failedCount = 0;

        for (PartnerSettlementInfo snapshot : snapshots) {
            try {
                ReconciliationResult result = activities.reconcilePartner(snapshot.partnerId(), date);
                if (!result.success()) {
                    failedCount++;
                    discrepancies.add(result.discrepancy());
                }
            } catch (Exception e) {
                failedCount++;
                discrepancies.add("검증 실패: partnerId=" + snapshot.partnerId() + ", error=" + e.getMessage());
                Workflow.getLogger(SettlementReconciliationWorkflowImpl.class)
                        .warn("파트너 정산 검증 실패: partnerId={}", snapshot.partnerId(), e);
            }
        }

        // Step 3: 불일치 보고
        if (!discrepancies.isEmpty()) {
            status = "REPORTING_DISCREPANCIES";
            activities.reportDiscrepancies(date, discrepancies);
        }

        status = "COMPLETED";
        return new ReconciliationReport(date, snapshots.size(), failedCount, discrepancies);
    }

    @Override
    public String getStatus() {
        return status;
    }
}
