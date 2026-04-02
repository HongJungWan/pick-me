package com.pickme.common.dlt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(KafkaTemplate.class)
public class DeadLetterConsumer {

    private final DeadLetterEventRepository repository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "pickme.dead-letter", groupId = "dlt-monitor")
    public void consume(String message, Acknowledgment ack) {
        try {
            JsonNode root = objectMapper.readTree(message);
            UUID eventId = UUID.fromString(root.path("eventId").asText());
            String eventType = root.path("eventType").asText();

            DeadLetterEvent dlt = new DeadLetterEvent(
                    eventId, eventType, "unknown", message, "DLT에 적재됨"
            );
            repository.save(dlt);

            log.warn("DLT 이벤트 저장: eventId={}, type={}", eventId, eventType);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("DLT 이벤트 처리 실패: {}", message, e);
            ack.acknowledge();
        }
    }
}
