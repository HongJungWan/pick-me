package com.pickme.partner.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface JpaPartnerRepository extends JpaRepository<PartnerJpaEntity, UUID> {}
