package com.pickme.orchestration.config;

import com.pickme.orchestration.activity.OrderActivities;
import com.pickme.orchestration.activity.OrderActivitiesImpl;
import com.pickme.orchestration.activity.ShadowOrderActivitiesImpl;
import com.pickme.orchestration.workflow.OrderFulfillmentWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "pickme.temporal.enabled", havingValue = "true")
public class TemporalWorkerBootstrap {

    private WorkerFactory workerFactory;

    @Bean
    public WorkerFactory workerFactory(WorkflowClient workflowClient,
                                       TemporalProperties properties,
                                       OrderActivitiesImpl orderActivities,
                                       Optional<ShadowOrderActivitiesImpl> shadowActivities) {
        workerFactory = WorkerFactory.newInstance(workflowClient);

        boolean shadowMode = properties.isShadowMode();
        OrderActivities activities;
        if (shadowMode) {
            activities = shadowActivities.orElseThrow(() ->
                    new IllegalStateException("pickme.temporal.shadow-mode=true이나 ShadowOrderActivitiesImpl 빈이 없습니다"));
        } else {
            activities = orderActivities;
        }
        String mode = shadowMode ? "SHADOW" : "LIVE";

        Worker orderSagaWorker = workerFactory.newWorker(properties.getTaskQueues().getOrderSaga());
        orderSagaWorker.registerWorkflowImplementationTypes(OrderFulfillmentWorkflowImpl.class);
        orderSagaWorker.registerActivitiesImplementations(activities);

        workerFactory.start();
        log.info("Temporal Worker 시작 완료: taskQueue={}, mode={}", properties.getTaskQueues().getOrderSaga(), mode);

        return workerFactory;
    }

    @PreDestroy
    public void shutdown() {
        if (workerFactory != null) {
            workerFactory.shutdown();
            log.info("Temporal Worker 종료 완료");
        }
    }
}
