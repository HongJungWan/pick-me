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
@Table(name = "product_snapshot", schema = "order_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSnapshotEntity {

    @Id
    private UUID productId;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    private long sellingPrice;

    @Column(nullable = false)
    private Instant updatedAt;

    public ProductSnapshotEntity(UUID productId, String productName, long sellingPrice) {
        this.productId = productId;
        this.productName = productName;
        this.sellingPrice = sellingPrice;
        this.updatedAt = Instant.now();
    }

    public void update(String productName, long sellingPrice) {
        this.productName = productName;
        this.sellingPrice = sellingPrice;
        this.updatedAt = Instant.now();
    }
}
