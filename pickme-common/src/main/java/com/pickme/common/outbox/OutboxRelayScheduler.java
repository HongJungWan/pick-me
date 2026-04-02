package com.pickme.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(KafkaTemplate.class)
public class OutboxRelayScheduler {

    private static final int MAX_RETRY = 5;
    private static final int BATCH_SIZE = 10;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void relay() {
        List<OutboxEvent> events = outboxRepository.findUnpublishedEvents(MAX_RETRY);
        if (events.isEmpty()) {
            return;
        }

        List<OutboxEvent> batch = events.stream().limit(BATCH_SIZE).toList();

        for (OutboxEvent event : batch) {
            try {
                String topic = resolveTopicName(event.getAggregateType());
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload()).get();
                event.markPublished();
                log.debug("Outbox event published: eventId={}, topic={}", event.getEventId(), topic);
            } catch (Exception e) {
                event.incrementRetryCount();
                log.warn("Failed to publish outbox event: eventId={}, retry={}", event.getEventId(), event.getRetryCount(), e);
            }
        }
    }

    private String resolveTopicName(String aggregateType) {
        return "pickme." + aggregateType.toLowerCase() + ".events";
    }
}
