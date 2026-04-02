package com.pickme.common.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyFilter {

    private final ProcessedEventRepository processedEventRepository;

    public boolean isDuplicate(UUID eventId) {
        return processedEventRepository.existsByEventId(eventId);
    }

    @Transactional
    public void markProcessed(UUID eventId, String eventType) {
        if (!isDuplicate(eventId)) {
            processedEventRepository.save(new ProcessedEvent(eventId, eventType));
            log.debug("Event marked as processed: eventId={}, type={}", eventId, eventType);
        }
    }
}
