package com.pickme.orchestration.workflow;

import com.pickme.orchestration.activity.SettlementActivities;
import com.pickme.orchestration.port.SettlementCommandPort.PartnerSettlementInfo;
import com.pickme.orchestration.port.SettlementCommandPort.ReconciliationResult;
import com.pickme.orchestration.workflow.SettlementReconciliationWorkflow.ReconciliationReport;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettlementReconciliationWorkflowTest {

    private static final String TASK_QUEUE = "test-settlement";
    private TestWorkflowEnvironment env;
    private WorkflowClient client;
    private SettlementActivities activities;

    @BeforeEach
    void setUp() {
        env = TestWorkflowEnvironment.newInstance();
        client = env.getWorkflowClient();
        activities = mock(SettlementActivities.class);
        Worker worker = env.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(SettlementReconciliationWorkflowImpl.class);
        worker.registerActivitiesImplementations(activities);
        env.start();
    }

    @AfterEach
    void tearDown() { env.close(); }

    @Test
    void 정상_정산_검증_불일치_없음() {
        LocalDate date = LocalDate.of(2026, 4, 8);
        UUID partnerId = UUID.randomUUID();

        when(activities.fetchDailySnapshots(date)).thenReturn(
                List.of(new PartnerSettlementInfo(partnerId, 100000, 5000, true)));
        when(activities.reconcilePartner(partnerId, date)).thenReturn(
                new ReconciliationResult(partnerId, true, null));

        SettlementReconciliationWorkflow wf = client.newWorkflowStub(
                SettlementReconciliationWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        ReconciliationReport report = wf.execute(date);

        assertThat(report.totalPartners()).isEqualTo(1);
        assertThat(report.failedPartners()).isEqualTo(0);
        assertThat(report.discrepancies()).isEmpty();
        verify(activities, never()).reportDiscrepancies(any(), any());
    }

    @Test
    void 불일치_탐지_시_보고() {
        LocalDate date = LocalDate.of(2026, 4, 8);
        UUID partnerId = UUID.randomUUID();

        when(activities.fetchDailySnapshots(date)).thenReturn(
                List.of(new PartnerSettlementInfo(partnerId, 100000, 5000, false)));
        when(activities.reconcilePartner(partnerId, date)).thenReturn(
                new ReconciliationResult(partnerId, false, "금액 불일치"));

        SettlementReconciliationWorkflow wf = client.newWorkflowStub(
                SettlementReconciliationWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        ReconciliationReport report = wf.execute(date);

        assertThat(report.failedPartners()).isEqualTo(1);
        assertThat(report.discrepancies()).containsExactly("금액 불일치");
        verify(activities).reportDiscrepancies(eq(date), any());
    }

    @Test
    void 빈_스냅샷_시_즉시_완료() {
        LocalDate date = LocalDate.of(2026, 4, 8);
        when(activities.fetchDailySnapshots(date)).thenReturn(List.of());

        SettlementReconciliationWorkflow wf = client.newWorkflowStub(
                SettlementReconciliationWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        ReconciliationReport report = wf.execute(date);

        assertThat(report.totalPartners()).isEqualTo(0);
        verify(activities, never()).reconcilePartner(any(), any());
    }
}
