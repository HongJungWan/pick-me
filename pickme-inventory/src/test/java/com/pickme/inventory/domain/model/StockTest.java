package com.pickme.inventory.domain.model;

import com.pickme.inventory.domain.event.InventoryReservedEvent;
import com.pickme.inventory.domain.event.InventoryRestoredEvent;
import com.pickme.inventory.domain.event.InventoryShortageEvent;
import com.pickme.inventory.domain.event.StockDepletedEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    @Test
    void 재고생성_초기수량_정상() {
        Stock stock = Stock.create(PRODUCT_ID, 100);

        assertThat(stock.getQuantity().getValue()).isEqualTo(100);
        assertThat(stock.getReservedQuantity().getValue()).isEqualTo(0);
        assertThat(stock.getTotalQuantity().getValue()).isEqualTo(100);
    }

    @Test
    void 재고예약_정상_수량감소_예약증가() {
        Stock stock = Stock.create(PRODUCT_ID, 50);
        stock.reserve(10, ORDER_ID);

        assertThat(stock.getQuantity().getValue()).isEqualTo(40);
        assertThat(stock.getReservedQuantity().getValue()).isEqualTo(10);
        assertThat(stock.getDomainEvents()).hasSize(1);
        assertThat(stock.getDomainEvents().get(0)).isInstanceOf(InventoryReservedEvent.class);
    }

    @Test
    void 재고예약_부족시_ShortageEvent발행() {
        Stock stock = Stock.create(PRODUCT_ID, 5);
        stock.reserve(10, ORDER_ID);

        assertThat(stock.getQuantity().getValue()).isEqualTo(5);
        assertThat(stock.getReservedQuantity().getValue()).isEqualTo(0);
        assertThat(stock.getDomainEvents()).hasSize(1);
        assertThat(stock.getDomainEvents().get(0)).isInstanceOf(InventoryShortageEvent.class);
    }

    @Test
    void 재고예약_전량소진시_DepletedEvent추가발행() {
        Stock stock = Stock.create(PRODUCT_ID, 10);
        stock.reserve(10, ORDER_ID);

        assertThat(stock.getQuantity().getValue()).isEqualTo(0);
        assertThat(stock.getDomainEvents()).hasSize(2);
        assertThat(stock.getDomainEvents().get(0)).isInstanceOf(InventoryReservedEvent.class);
        assertThat(stock.getDomainEvents().get(1)).isInstanceOf(StockDepletedEvent.class);
    }

    @Test
    void 재고확정_예약감소_총재고감소() {
        Stock stock = Stock.create(PRODUCT_ID, 50);
        stock.reserve(10, ORDER_ID);
        stock.confirm(10);

        assertThat(stock.getQuantity().getValue()).isEqualTo(40);
        assertThat(stock.getReservedQuantity().getValue()).isEqualTo(0);
        assertThat(stock.getTotalQuantity().getValue()).isEqualTo(40);
    }

    @Test
    void 재고취소_보상_예약감소_가용증가() {
        Stock stock = Stock.create(PRODUCT_ID, 50);
        stock.reserve(10, ORDER_ID);
        stock.clearDomainEvents();

        stock.cancel(10, ORDER_ID);

        assertThat(stock.getQuantity().getValue()).isEqualTo(50);
        assertThat(stock.getReservedQuantity().getValue()).isEqualTo(0);
        assertThat(stock.getDomainEvents()).hasSize(1);
        assertThat(stock.getDomainEvents().get(0)).isInstanceOf(InventoryRestoredEvent.class);
    }

    @Test
    void 입고_가용재고_총재고_증가() {
        Stock stock = Stock.create(PRODUCT_ID, 50);
        stock.restock(30);

        assertThat(stock.getQuantity().getValue()).isEqualTo(80);
        assertThat(stock.getTotalQuantity().getValue()).isEqualTo(80);
    }

    @Test
    void 수량_음수_예외발생() {
        assertThatThrownBy(() -> Quantity.of(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 이상");
    }

    @Test
    void 수량_부족_차감시_예외발생() {
        Quantity qty = Quantity.of(5);
        assertThatThrownBy(() -> qty.subtract(10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("수량 부족");
    }
}
