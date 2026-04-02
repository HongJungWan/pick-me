# pickme-order (Order Context)

> 주문 생성, 상태 관리, Saga 이벤트 오케스트레이션

## Aggregate Root — `Order`

| 메서드 | 설명 | 발행 이벤트 |
|--------|------|------------|
| `place()` | 주문 접수 (Factory) | OrderPlacedEvent |
| `completePayment()` | 결제 완료 → 주문 확정 | OrderConfirmedEvent |
| `cancel(reason)` | 주문 취소 (보상 트랜잭션) | OrderCancelledEvent |
| `requestRefund(reason)` | 환불 요청 | OrderRefundRequestedEvent |
| `startPreparing()` | 상품 준비 시작 | - |
| `ship()` | 발송 처리 | - |
| `deliver()` | 배송 완료 | - |

## 상태 전이

```
PLACED → PAYMENT_PENDING → PAID → PREPARING → SHIPPED → DELIVERED
  │            │
  └→ CANCELLED ←┘        PAID/PREPARING → REFUND_REQUESTED → REFUNDED
```

## Value Objects

- `OrderId` — UUID 식별자
- `OrderLine` — 상품ID, 상품명, 수량, 단가, 소계
- `Money` — 금액 (0 이상, add/subtract/multiply 연산)
- `ShippingInfo` — 수령인, 연락처, 주소
- `Address` — 우편번호, 도로명, 상세주소
- `OrderStatus` — 상태 전이 규칙 (`canTransitionTo()`)

## CQRS Read Model

- `order_schema.product_snapshot` — 상품 이벤트 구독 → 상품명/가격 스냅샷
- `order_schema.member_snapshot` — 회원 이벤트 구독 → 회원명/등급 스냅샷

## 구독 이벤트

| 이벤트 | 발행자 | 처리 |
|--------|--------|------|
| PaymentCompletedEvent | Payment | 주문 확정 |
| PaymentFailedEvent | Payment | 주문 취소 (보상) |
| InventoryShortageEvent | Inventory | 주문 취소 (보상) |
| ProductRegisteredEvent | Product | product_snapshot 갱신 |
| MemberRegisteredEvent | Member | member_snapshot 갱신 |

## API

| Method | URI | 설명 |
|--------|-----|------|
| POST | `/api/v1/orders` | 주문 생성 |
| GET | `/api/v1/orders/{id}` | 주문 조회 |
| GET | `/api/v1/orders?ordererId=` | 주문자별 조회 |
| PATCH | `/api/v1/orders/{id}/cancel` | 주문 취소 |

## 패키지 구조

```
pickme-order/
├── api/              OrderController, Request/Response DTO
├── application/      OrderService, OrderEventHandler, OrderSnapshotEventHandler
├── domain/
│   ├── model/        Order, OrderId, OrderLine, Money, ShippingInfo, Address, OrderStatus
│   ├── event/        OrderPlacedEvent, OrderConfirmedEvent, OrderCancelledEvent, OrderRefundRequestedEvent
│   └── repository/   OrderRepository (Interface)
└── infrastructure/
    ├── persistence/  JPA Entity, Mapper, Repository 구현체
    ├── messaging/    OrderSagaConsumer, OrderSnapshotConsumer
    └── snapshot/     ProductSnapshot, MemberSnapshot (CQRS Read Model)
```
