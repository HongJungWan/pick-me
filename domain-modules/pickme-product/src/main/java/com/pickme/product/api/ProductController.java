package com.pickme.product.api;

import com.pickme.product.api.request.CreateProductRequest;
import com.pickme.product.api.request.UpdateProductRequest;
import com.pickme.product.api.response.ProductResponse;
import com.pickme.product.application.ProductService;
import com.pickme.product.domain.model.Product;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        Product product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID productId) {
        Product product = productService.getProduct(productId);
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> products = productService.getAllProducts().stream()
                .map(ProductResponse::from)
                .toList();
        return ResponseEntity.ok(products);
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable UUID productId,
                                                         @Valid @RequestBody UpdateProductRequest request) {
        Product product = productService.updateProduct(productId, request);
        return ResponseEntity.ok(ProductResponse.from(product));
    }
}
