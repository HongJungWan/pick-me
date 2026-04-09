package com.pickme.orchestration.starter;

import com.pickme.orchestration.config.TemporalProperties;
import com.pickme.orchestration.dto.OrderFulfillmentRequest;
import com.pickme.orchestration.dto.OrderLineItem;
import com.pickme.orchestration.port.WorkflowStarter;
import com.pickme.orchestration.workflow.OrderFulfillmentWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pickme.temporal.enabled", havingValue = "true")
public class TemporalWorkflowStarter implements WorkflowStarter {

    private final WorkflowClient workflowClient;
    private final TemporalProperties properties;

    @Override
    public void startOrderFulfillment(UUID orderId, UUID ordererId,
                                      List<OrderLineItem> orderLines, long totalAmount, String paymentMethod) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId("order-fulfillment-" + orderId)
                .setTaskQueue(properties.getTaskQueues().getOrderSaga())
                .setWorkflowExecutionTimeout(Duration.ofMinutes(30))
                .build();

        OrderFulfillmentWorkflow workflow = workflowClient.newWorkflowStub(
                OrderFulfillmentWorkflow.class, options);

        WorkflowClient.start(workflow::execute,
                new OrderFulfillmentRequest(orderId, ordererId, orderLines, totalAmount, paymentMethod));

        log.info("주문 이행 워크플로우 시작: orderId={}, workflowId=order-fulfillment-{}", orderId, orderId);
    }

    @Override
    public void startRefund(UUID orderId, String reason, long refundAmount) {
        // Phase 3에서 구현 예정
        log.warn("환불 워크플로우 미구현: orderId={}", orderId);
    }
}
