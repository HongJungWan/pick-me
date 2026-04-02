# Event Catalog — pick-me 도메인 이벤트 명세

> 모든 모듈 간 통신은 이 카탈로그에 명시된 도메인 이벤트로만 수행됩니다.

## 이벤트 버전 관리 규칙

1. 모든 이벤트는 `EventEnvelope.version` 필드로 버전을 관리합니다 (현재 v1).
2. **하위 호환**: 필드 추가는 허용, 필드 삭제/타입 변경은 새 버전으로.
3. Consumer는 **알 수 없는 필드를 무시**해야 합니다 (forward compatibility).
4. 버전 변경 시 이 문서를 반드시 업데이트합니다.

---

## Order Context

| 이벤트 | v | Publisher | Consumers | Payload |
|--------|---|-----------|-----------|---------|
| OrderPlacedEvent | 1 | Order | Inventory, Payment, Notification | orderId, ordererId, orderLines[], totalAmount |
| OrderConfirmedEvent | 1 | Order | Inventory, Member, Notification | orderId, orderLines[] |
| OrderCancelledEvent | 1 | Order | Inventory, Payment, Notification | orderId, reason, orderLines[] |
| OrderRefundRequestedEvent | 1 | Order | Payment | orderId, refundAmount, reason |

## Payment Context

| 이벤트 | v | Publisher | Consumers | Payload |
|--------|---|-----------|-----------|---------|
| PaymentCompletedEvent | 1 | Payment | Order, Notification, Settlement, Member | paymentId, orderId, payerId, amount, paymentMethod, pgTransactionId |
| PaymentFailedEvent | 1 | Payment | Order, Notification | paymentId, orderId, reason |
| RefundCompletedEvent | 1 | Payment | Order, Notification, Settlement | paymentId, orderId, refundAmount |

## Product Context

| 이벤트 | v | Publisher | Consumers | Payload |
|--------|---|-----------|-----------|---------|
| ProductRegisteredEvent | 1 | Product | Inventory, Order(snapshot) | productId, productName, sellingPrice, partnerId |
| ProductInfoChangedEvent | 1 | Product | Order(snapshot) | productId, productName |
| ProductPriceChangedEvent | 1 | Product | Order(snapshot) | productId, oldPrice, newPrice |

## Inventory Context

| 이벤트 | v | Publisher | Consumers | Payload |
|--------|---|-----------|-----------|---------|
| InventoryReservedEvent | 1 | Inventory | Order | stockId, productId, orderId, reservedQuantity, remainingQuantity |
| InventoryShortageEvent | 1 | Inventory | Order, Notification | stockId, productId, orderId, requestedQuantity, availableQuantity |
| InventoryRestoredEvent | 1 | Inventory | — | stockId, productId, orderId, restoredQuantity |
| StockDepletedEvent | 1 | Inventory | Product, Notification | stockId, productId |

## Member Context

| 이벤트 | v | Publisher | Consumers | Payload |
|--------|---|-----------|-----------|---------|
| MemberRegisteredEvent | 1 | Member | Notification, Order(snapshot) | memberId, name, email |
| MemberGradeChangedEvent | 1 | Member | Order(snapshot), Notification | memberId, oldGrade, newGrade |

## Partner Context

| 이벤트 | v | Publisher | Consumers | Payload |
|--------|---|-----------|-----------|---------|
| PartnerApprovedEvent | 1 | Partner | Settlement | partnerId, companyName |
| PartnerSuspendedEvent | 1 | Partner | — | partnerId, reason |

## Settlement Context

| 이벤트 | v | Publisher | Consumers | Payload |
|--------|---|-----------|-----------|---------|
| SettlementCompletedEvent | 1 | Settlement | Notification | settlementId, partnerId, netSettlementAmount |
