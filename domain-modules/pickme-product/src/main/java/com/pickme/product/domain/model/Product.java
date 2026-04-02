package com.pickme.product.domain.model;

import com.pickme.common.event.DomainEvent;
import com.pickme.product.domain.event.ProductInfoChangedEvent;
import com.pickme.product.domain.event.ProductPriceChangedEvent;
import com.pickme.product.domain.event.ProductRegisteredEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Product implements com.pickme.common.event.DomainEventProvider {

    private final ProductId productId;
    private final UUID partnerId;
    private ProductName productName;
    private String description;
    private ProductPrice price;
    private Category category;
    private ProductStatus status;
    private final List<ProductOption> options;
    private final List<DomainEvent> domainEvents;

    private Product(ProductId productId, UUID partnerId, ProductName productName,
                    String description, ProductPrice price, Category category,
                    List<ProductOption> options) {
        this.productId = productId;
        this.partnerId = partnerId;
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.category = category;
        this.status = ProductStatus.DRAFT;
        this.options = new ArrayList<>(options);
        this.domainEvents = new ArrayList<>();
    }

    public static Product register(UUID partnerId, ProductName productName, String description,
                                   ProductPrice price, Category category, List<ProductOption> options) {
        Product product = new Product(
                ProductId.generate(), partnerId, productName, description, price, category, options
        );
        product.domainEvents.add(new ProductRegisteredEvent(
                product.productId.getValue(),
                productName.getValue(),
                price.getSellingPrice(),
                partnerId
        ));
        return product;
    }

    public static Product reconstitute(ProductId productId, UUID partnerId, ProductName productName,
                                       String description, ProductPrice price, Category category,
                                       ProductStatus status, List<ProductOption> options) {
        Product product = new Product(productId, partnerId, productName, description, price, category, options);
        product.status = status;
        return product;
    }

    public void changeName(ProductName newName) {
        this.productName = newName;
        this.domainEvents.add(new ProductInfoChangedEvent(
                this.productId.getValue(), newName.getValue()));
    }

    public void changePrice(ProductPrice newPrice) {
        long oldSellingPrice = this.price.getSellingPrice();
        this.price = newPrice;
        this.domainEvents.add(new ProductPriceChangedEvent(
                this.productId.getValue(), oldSellingPrice, newPrice.getSellingPrice()));
    }

    public void changeDescription(String newDescription) {
        this.description = newDescription;
        this.domainEvents.add(new ProductInfoChangedEvent(
                this.productId.getValue(), this.productName.getValue()));
    }

    public void changeStatus(ProductStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("상태 전이 불가: %s → %s", this.status, newStatus));
        }
        this.status = newStatus;
    }

    public void putOnSale() {
        changeStatus(ProductStatus.ON_SALE);
    }

    public void markSoldOut() {
        changeStatus(ProductStatus.SOLD_OUT);
    }

    public void hide() {
        changeStatus(ProductStatus.HIDDEN);
    }

    public void discontinue() {
        changeStatus(ProductStatus.DISCONTINUED);
    }

    public ProductId getProductId() { return productId; }
    public UUID getPartnerId() { return partnerId; }
    public ProductName getProductName() { return productName; }
    public String getDescription() { return description; }
    public ProductPrice getPrice() { return price; }
    public Category getCategory() { return category; }
    public ProductStatus getStatus() { return status; }
    public List<ProductOption> getOptions() { return Collections.unmodifiableList(options); }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
