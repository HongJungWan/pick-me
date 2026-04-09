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
        WorkflowServiceStubsOptions options = WorkflowServiceStubsOptions.newBuilder()
                .setTarget(properties.getTarget())
                .build();
        log.info("Temporal Server 연결: target={}", properties.getTarget());
        return WorkflowServiceStubs.newServiceStubs(options);
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
        WorkflowClientOptions options = WorkflowClientOptions.newBuilder()
                .setNamespace(properties.getNamespace())
                .build();
        return WorkflowClient.newInstance(serviceStubs, options);
    }
}
