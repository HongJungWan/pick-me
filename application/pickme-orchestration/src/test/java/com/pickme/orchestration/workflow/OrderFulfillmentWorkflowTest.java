package com.pickme.orchestration.workflow;

import com.pickme.orchestration.activity.OrderActivities;
import com.pickme.orchestration.dto.OrderFulfillmentRequest;
import com.pickme.orchestration.dto.OrderFulfillmentResult;
import com.pickme.orchestration.dto.OrderLineItem;
import com.pickme.orchestration.dto.PaymentResult;
import com.pickme.orchestration.dto.ReserveResult;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.failure.ActivityFailure;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderFulfillmentWorkflowTest {

    private static final String TASK_QUEUE = "test-order-fulfillment";

    private TestWorkflowEnvironment env;
    private WorkflowClient client;
    private OrderActivities activities;

    @BeforeEach
    void setUp() {
        env = TestWorkflowEnvironment.newInstance();
        client = env.getWorkflowClient();
        activities = mock(OrderActivities.class);

        Worker worker = env.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(OrderFulfillmentWorkflowImpl.class);
        worker.registerActivitiesImplementations(activities);
        env.start();
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    private OrderFulfillmentWorkflow createWorkflowStub() {
        return client.newWorkflowStub(
                OrderFulfillmentWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build()
        );
    }

    private OrderFulfillmentRequest createRequest() {
        return new OrderFulfillmentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(new OrderLineItem(UUID.randomUUID(), "상품A", 2, 10000)),
                20000,
                "CREDIT_CARD"
        );
    }

    @Test
    void 정상_경로_재고예약_결제_주문확정_재고확정() {
        OrderFulfillmentRequest request = createRequest();

        when(activities.reserveInventory(any(), any()))
                .thenReturn(ReserveResult.success(request.orderId()));
        when(activities.processPayment(any(), any(), anyLong(), anyString()))
                .thenReturn(PaymentResult.success(UUID.randomUUID()));

        OrderFulfillmentResult result = createWorkflowStub().execute(request);

        assertThat(result.success()).isTrue();
        assertThat(result.orderId()).isEqualTo(request.orderId());
        assertThat(result.paymentId()).isNotNull();
        assertThat(result.compensationFailed()).isFalse();

        verify(activities).reserveInventory(eq(request.orderId()), eq(request.orderLines()));
        verify(activities).confirmOrder(eq(request.orderId()));
        verify(activities).confirmInventory(eq(request.orderId()), eq(request.orderLines()));
        verify(activities, never()).restoreInventory(any(), any());
        verify(activities, never()).cancelOrder(any(), anyString());
    }

    @Test
    void 재고_부족_시_주문_취소() {
        OrderFulfillmentRequest request = createRequest();

        when(activities.reserveInventory(any(), any()))
                .thenReturn(ReserveResult.failure(request.orderId(), "재고 부족"));

        OrderFulfillmentResult result = createWorkflowStub().execute(request);

        assertThat(result.success()).isFalse();
        assertThat(result.failureType()).isEqualTo("INVENTORY_SHORTAGE");
        assertThat(result.compensationFailed()).isFalse();

        verify(activities).cancelOrder(eq(request.orderId()), anyString());
        verify(activities, never()).processPayment(any(), any(), anyLong(), anyString());
    }

    @Test
    void 결제_실패_시_재고_복원_후_주문_취소() {
        OrderFulfillmentRequest request = createRequest();

        when(activities.reserveInventory(any(), any()))
                .thenReturn(ReserveResult.success(request.orderId()));
        when(activities.processPayment(any(), any(), anyLong(), anyString()))
                .thenReturn(PaymentResult.failure("잔액 부족"));

        OrderFulfillmentResult result = createWorkflowStub().execute(request);

        assertThat(result.success()).isFalse();
        assertThat(result.failureType()).isEqualTo("PAYMENT_DECLINED");

        verify(activities).restoreInventory(eq(request.orderId()), eq(request.orderLines()));
        verify(activities).cancelOrder(eq(request.orderId()), anyString());
        verify(activities, never()).confirmOrder(any());
    }

    @Test
    void 주문확정_실패_시_환불_재고복원_주문취소_보상() {
        OrderFulfillmentRequest request = createRequest();

        when(activities.reserveInventory(any(), any()))
                .thenReturn(ReserveResult.success(request.orderId()));
        when(activities.processPayment(any(), any(), anyLong(), anyString()))
                .thenReturn(PaymentResult.success(UUID.randomUUID()));
        doThrow(new RuntimeException("DB 연결 실패"))
                .when(activities).confirmOrder(any());

        OrderFulfillmentResult result = createWorkflowStub().execute(request);

        assertThat(result.success()).isFalse();
        assertThat(result.failureType()).isEqualTo("CONFIRM_ORDER_FAILURE");

        // 보상: 환불 시도 + 재고 복원 + 주문 취소
        verify(activities).restoreInventory(eq(request.orderId()), eq(request.orderLines()));
        verify(activities).cancelOrder(eq(request.orderId()), anyString());
    }

    @Test
    void 워크플로우_상태_쿼리() {
        OrderFulfillmentRequest request = createRequest();

        when(activities.reserveInventory(any(), any()))
                .thenReturn(ReserveResult.success(request.orderId()));
        when(activities.processPayment(any(), any(), anyLong(), anyString()))
                .thenReturn(PaymentResult.success(UUID.randomUUID()));

        OrderFulfillmentWorkflow workflow = createWorkflowStub();
        workflow.execute(request);

        String status = workflow.getStatus();
        assertThat(status).isEqualTo("COMPLETED");
    }
}
