package com.pickme.inventory.application;

import com.pickme.common.event.DomainEventPublisher;
import com.pickme.common.idempotency.IdempotencyFilter;
import com.pickme.common.lock.DistributedLock;
import com.pickme.inventory.domain.model.Stock;
import com.pickme.inventory.domain.repository.StockRepository;
import com.pickme.inventory.infrastructure.config.StockRedisService;
import org.springframework.lang.Nullable;
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
    private final DomainEventPublisher eventPublisher;
    private final IdempotencyFilter idempotencyFilter;
    @Nullable
    private final StockRedisService stockRedisService;

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

    @DistributedLock(key = "'lock:inventory:stock:' + #productId")
    @Transactional
    public void handleOrderPlaced(UUID eventId, UUID orderId, UUID productId, int quantity) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다: productId=" + productId));

        stock.reserve(quantity, orderId);
        stockRepository.save(stock);
        if (stockRedisService != null) stockRedisService.syncFromDb(productId, stock.getQuantity().getValue());
        eventPublisher.publishAll(stock);

        idempotencyFilter.markProcessed(eventId, "OrderPlacedEvent");
        log.info("재고 예약 처리: productId={}, orderId={}, qty={}", productId, orderId, quantity);
    }

    @Transactional
    public void handleOrderConfirmed(UUID eventId, UUID orderId, UUID productId, int quantity) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다: productId=" + productId));

        stock.confirm(quantity);
        stockRepository.save(stock);

        idempotencyFilter.markProcessed(eventId, "OrderConfirmedEvent");
        log.info("재고 확정: productId={}, orderId={}, qty={}", productId, orderId, quantity);
    }

    @Transactional
    public void handleOrderCancelled(UUID eventId, UUID orderId, UUID productId, int quantity) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다: productId=" + productId));

        stock.cancel(quantity, orderId);
        stockRepository.save(stock);
        if (stockRedisService != null) stockRedisService.syncFromDb(productId, stock.getQuantity().getValue());
        eventPublisher.publishAll(stock);

        idempotencyFilter.markProcessed(eventId, "OrderCancelledEvent");
        log.info("재고 복원 (보상): productId={}, orderId={}, qty={}", productId, orderId, quantity);
    }
}
