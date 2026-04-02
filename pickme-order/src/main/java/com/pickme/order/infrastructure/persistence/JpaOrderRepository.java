package com.pickme.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {

    List<OrderJpaEntity> findByOrdererId(UUID ordererId);
}
