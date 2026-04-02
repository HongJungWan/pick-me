package com.pickme.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.published = false AND o.retryCount < :maxRetry ORDER BY o.createdAt ASC")
    List<OutboxEvent> findUnpublishedEvents(@Param("maxRetry") int maxRetry);
}
