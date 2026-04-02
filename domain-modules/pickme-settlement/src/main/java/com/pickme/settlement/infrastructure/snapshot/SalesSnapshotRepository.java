package com.pickme.settlement.infrastructure.snapshot;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.UUID;

public interface SalesSnapshotRepository extends JpaRepository<SalesSnapshotEntity, SalesSnapshotEntity.SalesSnapshotId> {

    default SalesSnapshotEntity findOrCreate(LocalDate date, UUID partnerId) {
        return findById(new SalesSnapshotEntity.SalesSnapshotId(date, partnerId))
                .orElseGet(() -> new SalesSnapshotEntity(date, partnerId));
    }
}
