package com.pickme.inventory.domain.repository;

import com.pickme.inventory.domain.model.Stock;
import com.pickme.inventory.domain.model.StockId;

import java.util.Optional;
import java.util.UUID;

public interface StockRepository {

    Stock save(Stock stock);

    Optional<Stock> findById(StockId stockId);

    Optional<Stock> findByProductId(UUID productId);
}
