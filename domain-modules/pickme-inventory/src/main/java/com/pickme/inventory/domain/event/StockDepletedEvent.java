package com.pickme.inventory.domain.event;

import com.pickme.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public class StockDepletedEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID stockId;
    private final UUID productId;
    private final Instant occurredAt;

    public StockDepletedEvent(UUID stockId, UUID productId) {
        this.eventId = UUID.randomUUID();
        this.stockId = stockId;
        this.productId = productId;
        this.occurredAt = Instant.now();
    }

    @Override public UUID getEventId() { return eventId; }
    @Override public String getEventType() { return "StockDepletedEvent"; }
    @Override public String getAggregateType() { return "inventory"; }
    @Override public String getAggregateId() { return stockId.toString(); }
    @Override public Instant getOccurredAt() { return occurredAt; }

    public UUID getStockId() { return stockId; }
    public UUID getProductId() { return productId; }
}
