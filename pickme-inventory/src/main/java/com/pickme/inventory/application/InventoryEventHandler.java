package com.pickme.inventory.application;

import com.pickme.common.idempotency.IdempotencyFilter;
import com.pickme.inventory.domain.model.Stock;
import com.pickme.inventory.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryEventHandler {

    private static final int DEFAULT_INITIAL_STOCK = 0;

    private final StockRepository stockRepository;
    private final IdempotencyFilter idempotencyFilter;

    @Transactional
    public void handleProductRegistered(UUID eventId, UUID productId, String productName) {
        if (idempotencyFilter.isDuplicate(eventId)) {
            log.info("중복 이벤트 무시: eventId={}", eventId);
            return;
        }

        if (stockRepository.findByProductId(productId).isPresent()) {
            log.info("이미 Stock이 존재합니다: productId={}", productId);
            idempotencyFilter.markProcessed(eventId, "ProductRegisteredEvent");
            return;
        }

        Stock stock = Stock.create(productId, DEFAULT_INITIAL_STOCK);
        stockRepository.save(stock);

        idempotencyFilter.markProcessed(eventId, "ProductRegisteredEvent");
        log.info("Stock 생성 완료: productId={}, stockId={}", productId, stock.getStockId());
    }
}
