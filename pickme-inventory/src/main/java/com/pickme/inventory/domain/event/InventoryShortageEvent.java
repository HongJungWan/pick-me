package com.pickme.inventory.domain.event;

import com.pickme.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public class InventoryShortageEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID stockId;
    private final UUID productId;
    private final UUID orderId;
    private final int requestedQuantity;
    private final int availableQuantity;
    private final Instant occurredAt;

    public InventoryShortageEvent(UUID stockId, UUID productId, UUID orderId,
                                  int requestedQuantity, int availableQuantity) {
        this.eventId = UUID.randomUUID();
        this.stockId = stockId;
        this.productId = productId;
        this.orderId = orderId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
        this.occurredAt = Instant.now();
    }

    @Override public UUID getEventId() { return eventId; }
    @Override public String getEventType() { return "InventoryShortageEvent"; }
    @Override public String getAggregateType() { return "inventory"; }
    @Override public String getAggregateId() { return stockId.toString(); }
    @Override public Instant getOccurredAt() { return occurredAt; }

    public UUID getStockId() { return stockId; }
    public UUID getProductId() { return productId; }
    public UUID getOrderId() { return orderId; }
    public int getRequestedQuantity() { return requestedQuantity; }
    public int getAvailableQuantity() { return availableQuantity; }
}
