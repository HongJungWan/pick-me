# pickme-inventory (Inventory Context)

> 재고 예약/확정/복원, 분산 락, Redis Pre-deduction

## Aggregate Root — `Stock`

| 메서드 | 설명 | 발행 이벤트 |
|--------|------|------------|
| `create()` | 재고 생성 (Factory) | - |
| `reserve(qty, orderId)` | 재고 예약 | InventoryReservedEvent / InventoryShortageEvent / StockDepletedEvent |
| `confirm(qty)` | 예약 확정 (실출고) | - |
| `cancel(qty, orderId)` | 예약 취소 (보상) | InventoryRestoredEvent |
| `restock(qty)` | 입고 | - |

## Value Objects

- `StockId` — UUID
- `Quantity` — 0 이상 정수, `subtract()` 시 부족하면 예외

## 동시성 제어

- **@DistributedLock**: `lock:inventory:stock:{productId}` (Redisson, TTL 5초)
- **Redis Pre-deduction**: Lua Script로 원자적 재고 차감 (`StockRedisService`)
- DB 재고 변경 후 `stockRedisService.syncFromDb()` 호출

## 구독 이벤트

| 이벤트 | 발행자 | 처리 |
|--------|--------|------|
| ProductRegisteredEvent | Product | Stock 자동 생성 |
| OrderPlacedEvent | Order | reserve() (orderLines 순회) |
| OrderConfirmedEvent | Order | confirm() |
| OrderCancelledEvent | Order | cancel() (보상) |
