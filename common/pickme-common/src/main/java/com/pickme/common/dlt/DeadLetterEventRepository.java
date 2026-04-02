package com.pickme.common.dlt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, Long> {

    Optional<DeadLetterEvent> findByEventId(UUID eventId);

    List<DeadLetterEvent> findByStatus(DeadLetterEvent.DltStatus status);
}
