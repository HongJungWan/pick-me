package com.pickme.common.dlt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/dlt")
@RequiredArgsConstructor
@ConditionalOnBean(KafkaTemplate.class)
public class DeadLetterAdminController {

    private final DeadLetterEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @GetMapping
    public ResponseEntity<List<DeadLetterEvent>> getPendingEvents() {
        return ResponseEntity.ok(repository.findByStatus(DeadLetterEvent.DltStatus.PENDING));
    }

    @PostMapping("/{eventId}/retry")
    public ResponseEntity<Map<String, String>> retryEvent(@PathVariable UUID eventId) {
        DeadLetterEvent dlt = repository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalArgumentException("DLT 이벤트를 찾을 수 없습니다: " + eventId));

        try {
            String topic = dlt.getOriginalTopic().equals("unknown")
                    ? resolveTopicFromEventType(dlt.getEventType())
                    : dlt.getOriginalTopic();

            kafkaTemplate.send(topic, dlt.getEventId().toString(), dlt.getPayload()).get();
            dlt.markRetried();
            repository.save(dlt);

            log.info("DLT 이벤트 재처리: eventId={}, topic={}", eventId, topic);
            return ResponseEntity.ok(Map.of("status", "retried", "eventId", eventId.toString()));
        } catch (Exception e) {
            dlt.markFailed();
            repository.save(dlt);
            log.error("DLT 이벤트 재처리 실패: eventId={}", eventId, e);
            return ResponseEntity.internalServerError().body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    private String resolveTopicFromEventType(String eventType) {
        if (eventType.contains("Order")) return "pickme.order.events";
        if (eventType.contains("Payment") || eventType.contains("Refund")) return "pickme.payment.events";
        if (eventType.contains("Product")) return "pickme.product.events";
        if (eventType.contains("Inventory") || eventType.contains("Stock")) return "pickme.inventory.events";
        if (eventType.contains("Member")) return "pickme.member.events";
        return "pickme.dead-letter";
    }
}
