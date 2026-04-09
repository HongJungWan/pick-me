package com.pickme.orchestration.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.time.LocalDate;
import java.util.List;

/**
 * 정산 Reconciliation 워크플로우.
 * 일일 스냅샷 조회 → 파트너별 검증 → 불일치 보고.
 * Temporal Schedules API로 매일 2AM 실행.
 */
@WorkflowInterface
public interface SettlementReconciliationWorkflow {

    @WorkflowMethod
    ReconciliationReport execute(LocalDate date);

    @QueryMethod
    String getStatus();

    record ReconciliationReport(LocalDate date, int totalPartners, int failedPartners,
                                List<String> discrepancies) {}
}
