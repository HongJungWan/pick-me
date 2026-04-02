package com.pickme.settlement.infrastructure.snapshot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.UUID;

public interface PartnerSnapshotRepository extends JpaRepository<PartnerSnapshotEntity, UUID> {

    default PartnerSnapshotEntity upsert(UUID partnerId, String companyName,
                                         BigDecimal commissionRate, String settlementCycle, String status) {
        return findById(partnerId)
                .map(snapshot -> {
                    snapshot.update(companyName, commissionRate, status);
                    return save(snapshot);
                })
                .orElseGet(() -> save(new PartnerSnapshotEntity(
                        partnerId, companyName, commissionRate, settlementCycle, status)));
    }
}
