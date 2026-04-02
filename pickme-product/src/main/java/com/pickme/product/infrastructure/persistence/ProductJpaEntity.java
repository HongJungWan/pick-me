package com.pickme.product.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products", schema = "product_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID partnerId;

    @Column(nullable = false, length = 200)
    private String productName;

    private String description;

    @Column(nullable = false)
    private long basePrice;

    @Column(nullable = false)
    private long discountedPrice;

    @Column(nullable = false, length = 50)
    private String categoryCode;

    @Column(nullable = false, length = 100)
    private String categoryName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatusJpa status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ProductOptionJpaEntity> options = new ArrayList<>();

    public ProductJpaEntity(UUID id, UUID partnerId, String productName, String description,
                            long basePrice, long discountedPrice, String categoryCode,
                            String categoryName, ProductStatusJpa status,
                            List<ProductOptionJpaEntity> options) {
        this.id = id;
        this.partnerId = partnerId;
        this.productName = productName;
        this.description = description;
        this.basePrice = basePrice;
        this.discountedPrice = discountedPrice;
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.options = options;
        options.forEach(o -> o.setProduct(this));
    }

    public void update(String productName, String description, long basePrice,
                       long discountedPrice, String categoryCode, String categoryName,
                       ProductStatusJpa status) {
        this.productName = productName;
        this.description = description;
        this.basePrice = basePrice;
        this.discountedPrice = discountedPrice;
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public enum ProductStatusJpa {
        DRAFT, ON_SALE, SOLD_OUT, HIDDEN, DISCONTINUED
    }
}
