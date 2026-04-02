package com.pickme.product.domain.event;

import com.pickme.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public class ProductRegisteredEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID productId;
    private final String productName;
    private final long sellingPrice;
    private final UUID partnerId;
    private final Instant occurredAt;

    public ProductRegisteredEvent(UUID productId, String productName, long sellingPrice, UUID partnerId) {
        this.eventId = UUID.randomUUID();
        this.productId = productId;
        this.productName = productName;
        this.sellingPrice = sellingPrice;
        this.partnerId = partnerId;
        this.occurredAt = Instant.now();
    }

    @Override
    public UUID getEventId() { return eventId; }

    @Override
    public String getEventType() { return "ProductRegisteredEvent"; }

    @Override
    public String getAggregateType() { return "product"; }

    @Override
    public String getAggregateId() { return productId.toString(); }

    @Override
    public Instant getOccurredAt() { return occurredAt; }

    public UUID getProductId() { return productId; }

    public String getProductName() { return productName; }

    public long getSellingPrice() { return sellingPrice; }

    public UUID getPartnerId() { return partnerId; }
}
