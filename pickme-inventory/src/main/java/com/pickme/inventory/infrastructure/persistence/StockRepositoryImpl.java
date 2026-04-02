package com.pickme.inventory.infrastructure.persistence;

import com.pickme.inventory.domain.model.Stock;
import com.pickme.inventory.domain.model.StockId;
import com.pickme.inventory.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StockRepositoryImpl implements StockRepository {

    private final JpaStockRepository jpaRepository;

    @Override
    public Stock save(Stock stock) {
        Optional<StockJpaEntity> existing = jpaRepository.findById(stock.getStockId().getValue());
        if (existing.isPresent()) {
            StockJpaEntity entity = existing.get();
            entity.update(
                    stock.getQuantity().getValue(),
                    stock.getReservedQuantity().getValue(),
                    stock.getTotalQuantity().getValue()
            );
            return StockMapper.toDomain(jpaRepository.save(entity));
        }
        StockJpaEntity entity = StockMapper.toJpaEntity(stock);
        return StockMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Stock> findById(StockId stockId) {
        return jpaRepository.findById(stockId.getValue()).map(StockMapper::toDomain);
    }

    @Override
    public Optional<Stock> findByProductId(UUID productId) {
        return jpaRepository.findByProductId(productId).map(StockMapper::toDomain);
    }
}
