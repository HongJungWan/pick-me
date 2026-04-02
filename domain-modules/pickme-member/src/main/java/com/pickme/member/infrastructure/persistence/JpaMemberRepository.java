package com.pickme.member.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaMemberRepository extends JpaRepository<MemberJpaEntity, UUID> {

    Optional<MemberJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
