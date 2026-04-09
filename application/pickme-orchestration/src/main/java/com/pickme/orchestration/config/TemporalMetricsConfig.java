package com.pickme.orchestration.config;

import com.uber.m3.tally.NoopScope;
import com.uber.m3.tally.Scope;
import io.micrometer.core.instrument.MeterRegistry;
import io.temporal.common.reporter.MicrometerClientStatsReporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "pickme.temporal.enabled", havingValue = "true")
public class TemporalMetricsConfig {

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    public Scope temporalMetricsScope(MeterRegistry meterRegistry) {
        log.info("Temporal 메트릭 → Micrometer/Prometheus 브릿지 구성");
        return new com.uber.m3.tally.RootScopeBuilder()
                .reporter(new MicrometerClientStatsReporter(meterRegistry))
                .reportEvery(com.uber.m3.util.Duration.ofSeconds(10));
    }
}
