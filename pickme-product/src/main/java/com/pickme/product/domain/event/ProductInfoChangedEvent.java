package com.pickme.product.domain.event;

import com.pickme.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public class ProductInfoChangedEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID productId;
    private final String productName;
    private final Instant occurredAt;

    public ProductInfoChangedEvent(UUID productId, String productName) {
        this.eventId = UUID.randomUUID();
        this.productId = productId;
        this.productName = productName;
        this.occurredAt = Instant.now();
    }

    @Override
    public UUID getEventId() { return eventId; }

    @Override
    public String getEventType() { return "ProductInfoChangedEvent"; }

    @Override
    public String getAggregateType() { return "product"; }

    @Override
    public String getAggregateId() { return productId.toString(); }

    @Override
    public Instant getOccurredAt() { return occurredAt; }

    public UUID getProductId() { return productId; }

    public String getProductName() { return productName; }
}
