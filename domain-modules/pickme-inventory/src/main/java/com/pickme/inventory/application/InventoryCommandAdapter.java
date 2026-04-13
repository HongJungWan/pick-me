package com.pickme.inventory.application;

import com.pickme.common.event.DomainEventPublisher;
import com.pickme.common.idempotency.IdempotencyFilter;
import com.pickme.common.lock.DistributedLock;
import com.pickme.inventory.application.port.StockCachePort;
import com.pickme.inventory.domain.model.Stock;
import com.pickme.inventory.domain.repository.StockRepository;
import com.pickme.orchestration.dto.OrderLineItem;
import com.pickme.orchestration.dto.ReserveResult;
import com.pickme.orchestration.port.InventoryCommandPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * InventoryCommandPort 구현체.
 * Temporal Activity에서 호출되며, 기존 재고 도메인 로직을 재사용한다.
 * @DistributedLock으로 동시 재고 예약 보호.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryCommandAdapter implements InventoryCommandPort {

    private final StockRepository stockRepository;
    private final DomainEventPublisher eventPublisher;
    private final IdempotencyFilter idempotencyFilter;
    @Nullable
    private final StockCachePort stockCache;

    @DistributedLock(key = "'lock:inventory:order:' + #orderId")
    @Transactional
    @Override
    public ReserveResult reserveInventory(UUID orderId, List<OrderLineItem> items) {
        UUID idempotencyKey = UUID.nameUUIDFromBytes(
                ("temporal-reserve:" + orderId).getBytes(StandardCharsets.UTF_8));

        if (idempotencyFilter.isDuplicate(idempotencyKey)) {
            log.info("중복 Activity 무시 (reserveInventory): orderId={}", orderId);
            return ReserveResult.success(orderId);
        }

        for (OrderLineItem item : items) {
            Stock stock = stockRepository.findByProductId(item.productId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "재고를 찾을 수 없습니다: productId=" + item.productId()));

            if (!stock.getQuantity().isGreaterThanOrEqual(item.quantity())) {
                return ReserveResult.failure(orderId,
                        "재고 부족: productId=" + item.productId()
                                + ", 요청=" + item.quantity()
                                + ", 가용=" + stock.getQuantity().getValue());
            }

            stock.reserve(item.quantity(), orderId);
            stockRepository.save(stock);
            if (stockCache != null) {
                stockCache.syncFromDb(item.productId(), stock.getQuantity().getValue());
            }
            eventPublisher.publishAll(stock);
        }

        idempotencyFilter.markProcessed(idempotencyKey, "TemporalReserveInventory");
        log.info("[CommandPort] 재고 예약 완료: orderId={}, items={}", orderId, items.size());
        return ReserveResult.success(orderId);
    }

    @Transactional
    @Override
    public void confirmInventory(UUID orderId, List<OrderLineItem> items) {
        UUID idempotencyKey = UUID.nameUUIDFromBytes(
                ("temporal-confirm-inv:" + orderId).getBytes(StandardCharsets.UTF_8));

        if (idempotencyFilter.isDuplicate(idempotencyKey)) {
            log.info("중복 Activity 무시 (confirmInventory): orderId={}", orderId);
            return;
        }

        for (OrderLineItem item : items) {
            Stock stock = stockRepository.findByProductId(item.productId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "재고를 찾을 수 없습니다: productId=" + item.productId()));

            stock.confirm(item.quantity());
            stockRepository.save(stock);
        }

        idempotencyFilter.markProcessed(idempotencyKey, "TemporalConfirmInventory");
        log.info("[CommandPort] 재고 확정: orderId={}", orderId);
    }

    @DistributedLock(key = "'lock:inventory:order:' + #orderId")
    @Transactional
    @Override
    public void restoreInventory(UUID orderId, List<OrderLineItem> items) {
        UUID idempotencyKey = UUID.nameUUIDFromBytes(
                ("temporal-restore:" + orderId).getBytes(StandardCharsets.UTF_8));

        if (idempotencyFilter.isDuplicate(idempotencyKey)) {
            log.info("중복 Activity 무시 (restoreInventory): orderId={}", orderId);
            return;
        }

        for (OrderLineItem item : items) {
            Stock stock = stockRepository.findByProductId(item.productId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "재고를 찾을 수 없습니다: productId=" + item.productId()));

            stock.cancel(item.quantity(), orderId);
            stockRepository.save(stock);
            if (stockCache != null) {
                stockCache.syncFromDb(item.productId(), stock.getQuantity().getValue());
            }
            eventPublisher.publishAll(stock);
        }

        idempotencyFilter.markProcessed(idempotencyKey, "TemporalRestoreInventory");
        log.info("[CommandPort] 재고 복원 (보상): orderId={}", orderId);
    }
}
