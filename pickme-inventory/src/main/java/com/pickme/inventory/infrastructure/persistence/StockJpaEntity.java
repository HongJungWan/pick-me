package com.pickme.inventory.infrastructure.persistence;

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
@Table(name = "stocks", schema = "inventory_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int reservedQuantity;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public StockJpaEntity(UUID id, UUID productId, int quantity, int reservedQuantity, int totalQuantity) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
        this.totalQuantity = totalQuantity;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void update(int quantity, int reservedQuantity, int totalQuantity) {
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
        this.totalQuantity = totalQuantity;
        this.updatedAt = Instant.now();
    }
}
