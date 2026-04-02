package com.pickme.inventory.infrastructure.persistence;

import com.pickme.inventory.domain.model.Stock;
import com.pickme.inventory.domain.model.StockId;

public final class StockMapper {

    private StockMapper() {}

    public static StockJpaEntity toJpaEntity(Stock stock) {
        return new StockJpaEntity(
                stock.getStockId().getValue(),
                stock.getProductId(),
                stock.getQuantity().getValue(),
                stock.getReservedQuantity().getValue(),
                stock.getTotalQuantity().getValue()
        );
    }

    public static Stock toDomain(StockJpaEntity entity) {
        return Stock.reconstitute(
                StockId.of(entity.getId()),
                entity.getProductId(),
                entity.getQuantity(),
                entity.getReservedQuantity(),
                entity.getTotalQuantity()
        );
    }
}
