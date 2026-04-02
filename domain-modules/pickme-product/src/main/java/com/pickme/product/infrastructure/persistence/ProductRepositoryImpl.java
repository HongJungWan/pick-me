package com.pickme.product.infrastructure.persistence;

import com.pickme.product.domain.model.Product;
import com.pickme.product.domain.model.ProductId;
import com.pickme.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final JpaProductRepository jpaRepository;

    @Override
    public Product save(Product product) {
        ProductJpaEntity entity = ProductMapper.toJpaEntity(product);
        ProductJpaEntity saved = jpaRepository.save(entity);
        return ProductMapper.toDomain(saved);
    }

    @Override
    public Optional<Product> findById(ProductId productId) {
        return jpaRepository.findById(productId.getValue())
                .map(ProductMapper::toDomain);
    }

    @Override
    public List<Product> findAll() {
        return jpaRepository.findAll().stream()
                .map(ProductMapper::toDomain)
                .toList();
    }
}
