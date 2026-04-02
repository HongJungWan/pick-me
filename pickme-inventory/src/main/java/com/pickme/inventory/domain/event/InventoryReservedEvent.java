package com.pickme.inventory.domain.event;

import com.pickme.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public class InventoryReservedEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID stockId;
    private final UUID productId;
    private final UUID orderId;
    private final int reservedQuantity;
    private final int remainingQuantity;
    private final Instant occurredAt;

    public InventoryReservedEvent(UUID stockId, UUID productId, UUID orderId,
                                  int reservedQuantity, int remainingQuantity) {
        this.eventId = UUID.randomUUID();
        this.stockId = stockId;
        this.productId = productId;
        this.orderId = orderId;
        this.reservedQuantity = reservedQuantity;
        this.remainingQuantity = remainingQuantity;
        this.occurredAt = Instant.now();
    }

    @Override public UUID getEventId() { return eventId; }
    @Override public String getEventType() { return "InventoryReservedEvent"; }
    @Override public String getAggregateType() { return "inventory"; }
    @Override public String getAggregateId() { return stockId.toString(); }
    @Override public Instant getOccurredAt() { return occurredAt; }

    public UUID getStockId() { return stockId; }
    public UUID getProductId() { return productId; }
    public UUID getOrderId() { return orderId; }
    public int getReservedQuantity() { return reservedQuantity; }
    public int getRemainingQuantity() { return remainingQuantity; }
}
