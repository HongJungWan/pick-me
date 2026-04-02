package com.pickme.inventory.domain.model;

import com.pickme.common.event.DomainEvent;
import com.pickme.inventory.domain.event.InventoryReservedEvent;
import com.pickme.inventory.domain.event.InventoryRestoredEvent;
import com.pickme.inventory.domain.event.InventoryShortageEvent;
import com.pickme.inventory.domain.event.StockDepletedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Stock {

    private final StockId stockId;
    private final UUID productId;
    private Quantity quantity;
    private Quantity reservedQuantity;
    private Quantity totalQuantity;
    private final List<DomainEvent> domainEvents;

    private Stock(StockId stockId, UUID productId, Quantity quantity,
                  Quantity reservedQuantity, Quantity totalQuantity) {
        this.stockId = stockId;
        this.productId = productId;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
        this.totalQuantity = totalQuantity;
        this.domainEvents = new ArrayList<>();
    }

    public static Stock create(UUID productId, int initialQuantity) {
        Quantity qty = Quantity.of(initialQuantity);
        return new Stock(StockId.generate(), productId, qty, Quantity.zero(), qty);
    }

    public static Stock reconstitute(StockId stockId, UUID productId,
                                     int quantity, int reservedQuantity, int totalQuantity) {
        return new Stock(stockId, productId,
                Quantity.of(quantity), Quantity.of(reservedQuantity), Quantity.of(totalQuantity));
    }

    public void reserve(int qty, UUID orderId) {
        if (!quantity.isGreaterThanOrEqual(qty)) {
            domainEvents.add(new InventoryShortageEvent(
                    stockId.getValue(), productId, orderId, qty, quantity.getValue()));
            return;
        }
        this.quantity = this.quantity.subtract(qty);
        this.reservedQuantity = this.reservedQuantity.add(qty);
        domainEvents.add(new InventoryReservedEvent(
                stockId.getValue(), productId, orderId, qty, quantity.getValue()));

        if (quantity.isZero()) {
            domainEvents.add(new StockDepletedEvent(stockId.getValue(), productId));
        }
    }

    public void confirm(int qty) {
        this.reservedQuantity = this.reservedQuantity.subtract(qty);
        this.totalQuantity = this.totalQuantity.subtract(qty);
    }

    public void cancel(int qty, UUID orderId) {
        this.reservedQuantity = this.reservedQuantity.subtract(qty);
        this.quantity = this.quantity.add(qty);
        domainEvents.add(new InventoryRestoredEvent(stockId.getValue(), productId, orderId, qty));
    }

    public void restock(int qty) {
        this.quantity = this.quantity.add(qty);
        this.totalQuantity = this.totalQuantity.add(qty);
    }

    public StockId getStockId() { return stockId; }
    public UUID getProductId() { return productId; }
    public Quantity getQuantity() { return quantity; }
    public Quantity getReservedQuantity() { return reservedQuantity; }
    public Quantity getTotalQuantity() { return totalQuantity; }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
