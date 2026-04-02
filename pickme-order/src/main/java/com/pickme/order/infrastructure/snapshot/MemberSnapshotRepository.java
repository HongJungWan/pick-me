package com.pickme.order.infrastructure.snapshot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MemberSnapshotRepository extends JpaRepository<MemberSnapshotEntity, UUID> {
}
