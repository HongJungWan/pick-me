package com.pickme.orchestration.workflow;

import com.pickme.orchestration.activity.RefundActivities;
import com.pickme.orchestration.dto.OrderLineItem;
import com.pickme.orchestration.dto.RefundRequest;
import com.pickme.orchestration.dto.RefundResult;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderRefundWorkflowTest {

    private static final String TASK_QUEUE = "test-refund";
    private TestWorkflowEnvironment env;
    private WorkflowClient client;
    private RefundActivities activities;

    @BeforeEach
    void setUp() {
        env = TestWorkflowEnvironment.newInstance();
        client = env.getWorkflowClient();
        activities = mock(RefundActivities.class);
        Worker worker = env.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(OrderRefundWorkflowImpl.class);
        worker.registerActivitiesImplementations(activities);
        env.start();
    }

    @AfterEach
    void tearDown() { env.close(); }

    private RefundRequest createRequest() {
        return new RefundRequest(UUID.randomUUID(), UUID.randomUUID(), "고객 요청",
                20000, List.of(new OrderLineItem(UUID.randomUUID(), "상품A", 2, 10000)));
    }

    @Test
    void 정상_환불_흐름() {
        RefundRequest req = createRequest();
        OrderRefundWorkflow wf = client.newWorkflowStub(OrderRefundWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        RefundResult result = wf.execute(req);

        assertThat(result.success()).isTrue();
        verify(activities).requestRefund(eq(req.orderId()), eq("고객 요청"));
        verify(activities).processRefund(eq(req.orderId()), eq(20000L));
        verify(activities).restoreInventory(eq(req.orderId()), eq(req.orderLines()));
        verify(activities).completeRefund(eq(req.orderId()));
    }

    @Test
    void PG_환불_실패_시_워크플로우_실패() {
        RefundRequest req = createRequest();
        doThrow(new RuntimeException("PG 타임아웃")).when(activities).processRefund(any(), anyLong());

        OrderRefundWorkflow wf = client.newWorkflowStub(OrderRefundWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        RefundResult result = wf.execute(req);

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).contains("PG 환불 처리 실패");
    }

    @Test
    void 환불_요청_실패_시_즉시_종료() {
        RefundRequest req = createRequest();
        doThrow(new RuntimeException("주문 상태 전이 불가")).when(activities).requestRefund(any(), anyString());

        OrderRefundWorkflow wf = client.newWorkflowStub(OrderRefundWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        RefundResult result = wf.execute(req);

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).contains("환불 요청 실패");
    }
}
