package com.pickme.product.infrastructure.config;

import com.pickme.product.application.ProductService;
import com.pickme.product.domain.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmUpRunner {

    private static final int WARM_UP_SIZE = 100;

    private final ProductService productService;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        try {
            List<Product> products = productService.getAllProducts();
            int count = Math.min(products.size(), WARM_UP_SIZE);

            for (int i = 0; i < count; i++) {
                Product product = products.get(i);
                productService.getProduct(product.getProductId().getValue());
            }

            log.info("Cache Warm-up 완료: {}개 상품 캐시 사전 로딩", count);
        } catch (Exception e) {
            log.warn("Cache Warm-up 실패 (앱 기동에 영향 없음): {}", e.getMessage());
        }
    }
}
