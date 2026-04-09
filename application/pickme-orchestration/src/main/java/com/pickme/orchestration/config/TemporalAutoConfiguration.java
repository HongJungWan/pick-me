package com.pickme.orchestration.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.common.metadata.POJOWorkflowImplMetadata;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(TemporalProperties.class)
@ConditionalOnProperty(name = "pickme.temporal.enabled", havingValue = "true")
public class TemporalAutoConfiguration {

    private final TemporalProperties properties;

    @Bean(destroyMethod = "shutdown")
    public WorkflowServiceStubs workflowServiceStubs() {
        try {
            WorkflowServiceStubsOptions options = WorkflowServiceStubsOptions.newBuilder()
                    .setTarget(properties.getTarget())
                    .build();
            WorkflowServiceStubs stubs = WorkflowServiceStubs.newServiceStubs(options);
            log.info("Temporal Server 연결 성공: target={}", properties.getTarget());
            return stubs;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Temporal Server 연결 실패 (target=" + properties.getTarget() + "). "
                    + "pickme.temporal.enabled=false로 설정하거나 Temporal Server를 시작하세요.", e);
        }
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
        WorkflowClientOptions options = WorkflowClientOptions.newBuilder()
                .setNamespace(properties.getNamespace())
                .build();
        return WorkflowClient.newInstance(serviceStubs, options);
    }
}
