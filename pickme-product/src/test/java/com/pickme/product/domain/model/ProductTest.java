package com.pickme.product.domain.model;

import com.pickme.product.domain.event.ProductRegisteredEvent;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    void 상품등록_정상요청_ProductRegisteredEvent발행() {
        Product product = Product.register(
                UUID.randomUUID(),
                new ProductName("테스트 상품"),
                "상품 설명",
                ProductPrice.of(29900),
                new Category("ELEC", "전자제품"),
                Collections.emptyList()
        );

        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.getProductId()).isNotNull();
        assertThat(product.getDomainEvents()).hasSize(1);
        assertThat(product.getDomainEvents().get(0)).isInstanceOf(ProductRegisteredEvent.class);
    }

    @Test
    void 상품명_빈문자열_예외발생() {
        assertThatThrownBy(() -> new ProductName(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비어있을 수 없습니다");
    }

    @Test
    void 상품명_200자초과_예외발생() {
        String longName = "a".repeat(201);
        assertThatThrownBy(() -> new ProductName(longName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("200자");
    }

    @Test
    void 가격_할인가가_기본가_초과_예외발생() {
        assertThatThrownBy(() -> ProductPrice.of(10000, 20000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("초과할 수 없습니다");
    }

    @Test
    void 가격_할인율_계산() {
        ProductPrice price = ProductPrice.of(10000, 8000);
        assertThat(price.getSellingPrice()).isEqualTo(8000);
        assertThat(price.getDiscountRate().intValue()).isEqualTo(20);
    }

    @Test
    void 상태전이_DRAFT에서_ON_SALE_성공() {
        Product product = createDraftProduct();
        product.putOnSale();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    void 상태전이_DRAFT에서_DISCONTINUED_실패() {
        Product product = createDraftProduct();
        assertThatThrownBy(product::discontinue)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("상태 전이 불가");
    }

    @Test
    void 상태전이_ON_SALE에서_SOLD_OUT_성공() {
        Product product = createDraftProduct();
        product.putOnSale();
        product.markSoldOut();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
    }

    @Test
    void 상태전이_DISCONTINUED에서_전이불가() {
        Product product = createDraftProduct();
        product.putOnSale();
        product.discontinue();
        assertThatThrownBy(product::putOnSale)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 카테고리_코드_빈값_예외발생() {
        assertThatThrownBy(() -> new Category("", "전자제품"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 옵션_옵션명_빈값_예외발생() {
        assertThatThrownBy(() -> new ProductOption("", "빨강", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 옵션_추가금액_음수_예외발생() {
        assertThatThrownBy(() -> new ProductOption("색상", "빨강", -1000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Product createDraftProduct() {
        return Product.register(
                UUID.randomUUID(),
                new ProductName("테스트 상품"),
                "설명",
                ProductPrice.of(10000),
                new Category("ELEC", "전자제품"),
                Collections.emptyList()
        );
    }
}
