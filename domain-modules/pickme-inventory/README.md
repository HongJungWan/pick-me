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

## 이벤트 흐름

### 발행 이벤트 → Kafka 토픽

| 이벤트 | 토픽 | 소비자 |
|--------|------|--------|
| InventoryReservedEvent | `pickme.inventory.events` | (현재 직접 소비자 없음 — 향후 확장) |
| InventoryShortageEvent | `pickme.inventory.events` | Order (주문 취소 보상), Notification (운영 알림) |
| InventoryRestoredEvent | `pickme.inventory.events` | (현재 직접 소비자 없음 — 향후 확장) |
| StockDepletedEvent | `pickme.inventory.events` | Notification (운영 알림) |

### 구독 이벤트

| 이벤트 | 발행자 | 토픽 | 처리 |
|--------|--------|------|------|
| ProductRegisteredEvent | Product | `pickme.product.events` | Stock 자동 생성 |
| OrderPlacedEvent | Order | `pickme.order.events` | reserve() (orderLines 순회) |
| OrderConfirmedEvent | Order | `pickme.order.events` | confirm() (예약 확정) |
| OrderCancelledEvent | Order | `pickme.order.events` | cancel() (재고 복원 보상) |

## 패키지 구조

```
pickme-inventory/
├── api/              InventoryController, Request/Response DTO
├── application/      InventoryService, InventoryEventHandler, InventoryCommandAdapter
├── domain/
│   ├── model/        Stock, StockId, Quantity
│   ├── event/        InventoryReservedEvent, InventoryShortageEvent, InventoryRestoredEvent, StockDepletedEvent
│   └── repository/   StockRepository (Interface)
└── infrastructure/
    ├── persistence/  JPA Entity, Mapper, Repository 구현체
    ├── messaging/    InventoryEventConsumer (사가 이벤트는 Temporal 시 스킵, ProductRegisteredEvent는 항상 활성)
    └── redis/        StockRedisService (Lua Script 원자적 차감 + DB 동기화)
```

## Temporal 연동

`InventoryCommandAdapter`가 `InventoryCommandPort`를 구현하여 Temporal Activity에서 호출된다.

| 메서드 | 분산 락 | 멱등성 키 |
|--------|--------|----------|
| `reserveInventory(orderId, items)` | `@DistributedLock(key=orderId)` | `temporal-reserve:{orderId}` |
| `confirmInventory(orderId, items)` | - | `temporal-confirm-inv:{orderId}` |
| `restoreInventory(orderId, items)` | `@DistributedLock(key=orderId)` | `temporal-restore:{orderId}` |
