package com.pickme.inventory.domain.event;

import com.pickme.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public class InventoryRestoredEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID stockId;
    private final UUID productId;
    private final UUID orderId;
    private final int restoredQuantity;
    private final Instant occurredAt;

    public InventoryRestoredEvent(UUID stockId, UUID productId, UUID orderId, int restoredQuantity) {
        this.eventId = UUID.randomUUID();
        this.stockId = stockId;
        this.productId = productId;
        this.orderId = orderId;
        this.restoredQuantity = restoredQuantity;
        this.occurredAt = Instant.now();
    }

    @Override public UUID getEventId() { return eventId; }
    @Override public String getEventType() { return "InventoryRestoredEvent"; }
    @Override public String getAggregateType() { return "inventory"; }
    @Override public String getAggregateId() { return stockId.toString(); }
    @Override public Instant getOccurredAt() { return occurredAt; }

    public UUID getStockId() { return stockId; }
    public UUID getProductId() { return productId; }
    public UUID getOrderId() { return orderId; }
    public int getRestoredQuantity() { return restoredQuantity; }
}
