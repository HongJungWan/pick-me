package com.pickme.common.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(KafkaTemplate.class)
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public Health health() {
        try {
            kafkaTemplate.getProducerFactory().createProducer().close();
            return Health.up()
                    .withDetail("bootstrap-servers", kafkaTemplate.getProducerFactory().getConfigurationProperties().get("bootstrap.servers"))
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
