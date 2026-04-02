package com.pickme.order.domain.model;

import java.util.Objects;
import java.util.UUID;

public class OrderLine {

    private final UUID productId;
    private final String productName;
    private final int quantity;
    private final Money unitPrice;
    private final Money lineTotal;

    public OrderLine(UUID productId, String productName, int quantity, Money unitPrice) {
        if (productId == null) throw new IllegalArgumentException("상품 ID는 null일 수 없습니다");
        if (productName == null || productName.isBlank()) throw new IllegalArgumentException("상품명은 비어있을 수 없습니다");
        if (quantity <= 0) throw new IllegalArgumentException("수량은 1 이상이어야 합니다");
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = unitPrice.multiply(quantity);
    }

    public UUID getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public Money getUnitPrice() { return unitPrice; }
    public Money getLineTotal() { return lineTotal; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderLine that = (OrderLine) o;
        return Objects.equals(productId, that.productId);
    }
    @Override public int hashCode() { return Objects.hash(productId); }
}
