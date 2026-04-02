package com.pickme.product.domain.event;

import com.pickme.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public class ProductPriceChangedEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID productId;
    private final long oldPrice;
    private final long newPrice;
    private final Instant occurredAt;

    public ProductPriceChangedEvent(UUID productId, long oldPrice, long newPrice) {
        this.eventId = UUID.randomUUID();
        this.productId = productId;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.occurredAt = Instant.now();
    }

    @Override
    public UUID getEventId() { return eventId; }

    @Override
    public String getEventType() { return "ProductPriceChangedEvent"; }

    @Override
    public String getAggregateType() { return "product"; }

    @Override
    public String getAggregateId() { return productId.toString(); }

    @Override
    public Instant getOccurredAt() { return occurredAt; }

    public UUID getProductId() { return productId; }

    public long getOldPrice() { return oldPrice; }

    public long getNewPrice() { return newPrice; }
}
