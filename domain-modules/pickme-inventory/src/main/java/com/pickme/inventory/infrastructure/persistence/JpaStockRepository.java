package com.pickme.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaStockRepository extends JpaRepository<StockJpaEntity, UUID> {

    Optional<StockJpaEntity> findByProductId(UUID productId);
}
