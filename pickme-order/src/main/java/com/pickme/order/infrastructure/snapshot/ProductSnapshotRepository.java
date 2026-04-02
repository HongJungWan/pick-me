package com.pickme.order.infrastructure.snapshot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductSnapshotRepository extends JpaRepository<ProductSnapshotEntity, UUID> {
}
