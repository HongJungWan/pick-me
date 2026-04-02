package com.pickme.product.domain.repository;

import com.pickme.product.domain.model.Product;
import com.pickme.product.domain.model.ProductId;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(ProductId productId);

    List<Product> findAll();
}
