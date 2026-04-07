package com.pickme.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxCleanupScheduler {

    private static final int RETENTION_DAYS = 7;

    private final OutboxRepository outboxRepository;

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupOldOutboxEvents() {
        Instant cutoff = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
        int deleted = outboxRepository.deleteByCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} outbox events older than {} days", deleted, RETENTION_DAYS);
        }
    }
}
