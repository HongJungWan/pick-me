package com.pickme.member.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "members", schema = "member_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GradeJpa grade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusJpa status;

    @Column(nullable = false)
    private long accumulatedPurchaseAmount;

    @Column(nullable = false)
    private Instant registeredAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public MemberJpaEntity(UUID id, String email, String password, String name, String phone,
                           GradeJpa grade, StatusJpa status, long accumulatedPurchaseAmount,
                           Instant registeredAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.grade = grade;
        this.status = status;
        this.accumulatedPurchaseAmount = accumulatedPurchaseAmount;
        this.registeredAt = registeredAt;
        this.updatedAt = Instant.now();
    }

    public void update(String name, String phone, String password, GradeJpa grade,
                       StatusJpa status, long accumulatedPurchaseAmount) {
        this.name = name;
        this.phone = phone;
        this.password = password;
        this.grade = grade;
        this.status = status;
        this.accumulatedPurchaseAmount = accumulatedPurchaseAmount;
        this.updatedAt = Instant.now();
    }

    public enum GradeJpa { NORMAL, SILVER, GOLD, VIP, VVIP }
    public enum StatusJpa { ACTIVE, DORMANT, WITHDRAWN }
}
