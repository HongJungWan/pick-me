package com.pickme.orchestration.config;

import com.pickme.orchestration.activity.OrderActivities;
import com.pickme.orchestration.activity.OrderActivitiesImpl;
import com.pickme.orchestration.activity.PartnerOnboardingActivitiesImpl;
import com.pickme.orchestration.activity.RefundActivitiesImpl;
import com.pickme.orchestration.activity.SettlementActivitiesImpl;
import com.pickme.orchestration.activity.ShadowOrderActivitiesImpl;
import com.pickme.orchestration.workflow.OrderFulfillmentWorkflowImpl;
import com.pickme.orchestration.workflow.OrderRefundWorkflowImpl;
import com.pickme.orchestration.workflow.PartnerOnboardingWorkflowImpl;
import com.pickme.orchestration.workflow.SettlementReconciliationWorkflowImpl;
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
                                       Optional<ShadowOrderActivitiesImpl> shadowActivities,
                                       RefundActivitiesImpl refundActivities,
                                       SettlementActivitiesImpl settlementActivities,
                                       PartnerOnboardingActivitiesImpl partnerActivities) {
        workerFactory = WorkerFactory.newInstance(workflowClient);

        // 주문 이행 사가 Worker
        boolean shadowMode = properties.isShadowMode();
        OrderActivities orderSagaActivities;
        if (shadowMode) {
            orderSagaActivities = shadowActivities.orElseThrow(() ->
                    new IllegalStateException("pickme.temporal.shadow-mode=true이나 ShadowOrderActivitiesImpl 빈이 없습니다"));
        } else {
            orderSagaActivities = orderActivities;
        }
        String mode = shadowMode ? "SHADOW" : "LIVE";

        Worker orderSagaWorker = workerFactory.newWorker(properties.getTaskQueues().getOrderSaga());
        orderSagaWorker.registerWorkflowImplementationTypes(OrderFulfillmentWorkflowImpl.class, OrderRefundWorkflowImpl.class);
        orderSagaWorker.registerActivitiesImplementations(orderSagaActivities, refundActivities);

        // 정산 Worker
        Worker settlementWorker = workerFactory.newWorker(properties.getTaskQueues().getSettlement());
        settlementWorker.registerWorkflowImplementationTypes(SettlementReconciliationWorkflowImpl.class);
        settlementWorker.registerActivitiesImplementations(settlementActivities);

        // 파트너 온보딩 Worker
        Worker partnerWorker = workerFactory.newWorker(properties.getTaskQueues().getPartner());
        partnerWorker.registerWorkflowImplementationTypes(PartnerOnboardingWorkflowImpl.class);
        partnerWorker.registerActivitiesImplementations(partnerActivities);

        workerFactory.start();
        log.info("Temporal Worker 시작 완료: orderSaga={} ({}), settlement={}, partner={}",
                properties.getTaskQueues().getOrderSaga(), mode,
                properties.getTaskQueues().getSettlement(),
                properties.getTaskQueues().getPartner());

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
