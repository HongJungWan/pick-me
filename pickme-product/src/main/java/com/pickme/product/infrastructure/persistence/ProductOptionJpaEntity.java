package com.pickme.product.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_options", schema = "product_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOptionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductJpaEntity product;

    @Column(nullable = false, length = 100)
    private String optionName;

    @Column(nullable = false, length = 200)
    private String optionValue;

    @Column(nullable = false)
    private long additionalPrice;

    public ProductOptionJpaEntity(String optionName, String optionValue, long additionalPrice) {
        this.optionName = optionName;
        this.optionValue = optionValue;
        this.additionalPrice = additionalPrice;
    }

    void setProduct(ProductJpaEntity product) {
        this.product = product;
    }
}
