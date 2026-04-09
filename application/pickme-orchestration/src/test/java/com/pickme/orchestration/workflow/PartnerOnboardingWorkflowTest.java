package com.pickme.orchestration.workflow;

import com.pickme.orchestration.activity.PartnerOnboardingActivities;
import com.pickme.orchestration.dto.PartnerOnboardingRequest;
import com.pickme.orchestration.dto.PartnerOnboardingResult;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartnerOnboardingWorkflowTest {

    private static final String TASK_QUEUE = "test-partner";
    private TestWorkflowEnvironment env;
    private WorkflowClient client;
    private PartnerOnboardingActivities activities;

    @BeforeEach
    void setUp() {
        env = TestWorkflowEnvironment.newInstance();
        client = env.getWorkflowClient();
        activities = mock(PartnerOnboardingActivities.class);
        Worker worker = env.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(PartnerOnboardingWorkflowImpl.class);
        worker.registerActivitiesImplementations(activities);
        env.start();
    }

    @AfterEach
    void tearDown() { env.close(); }

    private PartnerOnboardingRequest createRequest() {
        return new PartnerOnboardingRequest(
                "123-45-67890", "테스트컴퍼니", "홍길동",
                BigDecimal.valueOf(10), "MONTHLY",
                LocalDate.of(2026, 5, 1), LocalDate.of(2027, 4, 30));
    }

    @Test
    void 관리자_승인_시_파트너_생성_완료() {
        UUID partnerId = UUID.randomUUID();
        when(activities.registerPartner(any())).thenReturn(partnerId);

        PartnerOnboardingWorkflow wf = client.newWorkflowStub(
                PartnerOnboardingWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("partner-test-approve")
                        .setTaskQueue(TASK_QUEUE).build());

        // 비동기 실행 후 시그널 전송
        CompletableFuture<PartnerOnboardingResult> future = WorkflowClient.execute(wf::execute, createRequest());

        // 시그널 전송 (승인)
        PartnerOnboardingWorkflow stub = client.newWorkflowStub(
                PartnerOnboardingWorkflow.class, "partner-test-approve");
        stub.approveByAdmin();

        PartnerOnboardingResult result = future.join();

        assertThat(result.success()).isTrue();
        assertThat(result.partnerId()).isEqualTo(partnerId);
        assertThat(result.status()).isEqualTo("APPROVED");
        verify(activities).approvePartner(eq(partnerId));
    }

    @Test
    void 관리자_거절_시_파트너_거절_처리() {
        UUID partnerId = UUID.randomUUID();
        when(activities.registerPartner(any())).thenReturn(partnerId);

        PartnerOnboardingWorkflow wf = client.newWorkflowStub(
                PartnerOnboardingWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("partner-test-reject")
                        .setTaskQueue(TASK_QUEUE).build());

        CompletableFuture<PartnerOnboardingResult> future = WorkflowClient.execute(wf::execute, createRequest());

        PartnerOnboardingWorkflow stub = client.newWorkflowStub(
                PartnerOnboardingWorkflow.class, "partner-test-reject");
        stub.rejectByAdmin("사업자 등록증 불일치");

        PartnerOnboardingResult result = future.join();

        assertThat(result.success()).isFalse();
        assertThat(result.status()).isEqualTo("REJECTED");
        assertThat(result.failureReason()).isEqualTo("사업자 등록증 불일치");
        verify(activities).rejectPartner(eq(partnerId), eq("사업자 등록증 불일치"));
        verify(activities, never()).approvePartner(any());
    }

    @Test
    void 타임아웃_만료_시_자동_만료() {
        UUID partnerId = UUID.randomUUID();
        when(activities.registerPartner(any())).thenReturn(partnerId);

        PartnerOnboardingWorkflow wf = client.newWorkflowStub(
                PartnerOnboardingWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("partner-test-timeout")
                        .setTaskQueue(TASK_QUEUE).build());

        CompletableFuture<PartnerOnboardingResult> future = WorkflowClient.execute(wf::execute, createRequest());

        // 7일 시간 빨리 감기
        env.sleep(java.time.Duration.ofDays(7).plusMinutes(1));

        PartnerOnboardingResult result = future.join();

        assertThat(result.success()).isFalse();
        assertThat(result.status()).isEqualTo("EXPIRED");
        verify(activities).notifyExpired(eq(partnerId));
        verify(activities, never()).approvePartner(any());
    }
}
