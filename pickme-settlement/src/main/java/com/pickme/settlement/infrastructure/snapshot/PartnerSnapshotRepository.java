package com.pickme.settlement.infrastructure.snapshot;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PartnerSnapshotRepository extends JpaRepository<PartnerSnapshotEntity, UUID> {
}
