package com.pickme.product.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickme.common.event.DomainEvent;
import com.pickme.common.outbox.OutboxEvent;
import com.pickme.common.outbox.OutboxRepository;
import com.pickme.product.api.request.CreateProductRequest;
import com.pickme.product.api.request.UpdateProductRequest;
import com.pickme.product.domain.model.Category;
import com.pickme.product.domain.model.Product;
import com.pickme.product.domain.model.ProductId;
import com.pickme.product.domain.model.ProductName;
import com.pickme.product.domain.model.ProductOption;
import com.pickme.product.domain.model.ProductPrice;
import com.pickme.product.domain.model.ProductStatus;
import com.pickme.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Product createProduct(CreateProductRequest request) {
        List<ProductOption> options = request.options() != null
                ? request.options().stream()
                    .map(o -> new ProductOption(o.optionName(), o.optionValue(), o.additionalPrice()))
                    .toList()
                : Collections.emptyList();

        Product product = Product.register(
                request.partnerId(),
                new ProductName(request.productName()),
                request.description(),
                ProductPrice.of(request.basePrice(), request.discountedPrice()),
                new Category(request.categoryCode(), request.categoryName()),
                options
        );

        Product saved = productRepository.save(product);
        publishDomainEvents(product);
        return saved;
    }

    @Transactional(readOnly = true)
    public Product getProduct(UUID productId) {
        return productRepository.findById(ProductId.of(productId))
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional
    public Product updateProduct(UUID productId, UpdateProductRequest request) {
        Product product = productRepository.findById(ProductId.of(productId))
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        if (request.productName() != null) {
            product.changeName(new ProductName(request.productName()));
        }
        if (request.description() != null) {
            product.changeDescription(request.description());
        }
        if (request.basePrice() != null && request.discountedPrice() != null) {
            product.changePrice(ProductPrice.of(request.basePrice(), request.discountedPrice()));
        }
        if (request.status() != null) {
            product.changeStatus(ProductStatus.valueOf(request.status()));
        }

        Product saved = productRepository.save(product);
        publishDomainEvents(product);
        return saved;
    }

    private void publishDomainEvents(Product product) {
        for (DomainEvent event : product.getDomainEvents()) {
            try {
                String payload = objectMapper.writeValueAsString(event);
                outboxRepository.save(OutboxEvent.from(event, payload));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("이벤트 직렬화 실패", e);
            }
        }
        product.clearDomainEvents();
    }
}
