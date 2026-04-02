package com.pickme.order.infrastructure.snapshot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "member_snapshot", schema = "order_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSnapshotEntity {

    @Id
    private UUID memberId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 10)
    private String grade;

    @Column(nullable = false)
    private Instant updatedAt;

    public MemberSnapshotEntity(UUID memberId, String name, String grade) {
        this.memberId = memberId;
        this.name = name;
        this.grade = grade;
        this.updatedAt = Instant.now();
    }

    public void update(String name, String grade) {
        this.name = name;
        this.grade = grade;
        this.updatedAt = Instant.now();
    }
}
