# pickme-payment (Payment Context)

> PG 결제 처리, 환불, Domain Service 패턴

## Aggregate Root — `Payment`

| 메서드 | 설명 | 발행 이벤트 |
|--------|------|------------|
| `request()` | 결제 요청 (Factory) | - |
| `process()` | PG 처리 중 상태 전이 | - |
| `complete(pgResponse)` | 결제 완료 (PG 성공 가드) | PaymentCompletedEvent |
| `fail(reason)` | 결제 실패 | PaymentFailedEvent |
| `requestRefund()` | 환불 요청 | - |
| `refund()` | 환불 완료 | RefundCompletedEvent |

## 상태 전이

```
REQUESTED → PROCESSING → COMPLETED → REFUND_REQUESTED → REFUNDED
                │
                └→ FAILED
```

## Domain Service — `PaymentProcessingService`

PG 호출 + Payment 상태 전이를 단일 비즈니스 행위로 캡슐화. **Spring 어노테이션 없음** (도메인 순수성 유지).

```java
public Payment processNewPayment(UUID orderId, UUID payerId, long amount, PaymentMethod method) {
    Payment payment = Payment.request(...);
    payment.process();
    PgResponse response = pgGateway.requestPayment(...);
    if (response.isSuccess()) payment.complete(response);
    else payment.fail(response.getMessage());
    return payment;
}
```

Bean 등록은 `infrastructure/config/PaymentDomainConfig.java`에서 수행.

## ACL (Anti-Corruption Layer)

- `PgPaymentGateway` — PG사 API Port (PaymentProcessingService.PgGateway extends)
- `MockPgPaymentAdapter` — Mock 구현 + Resilience4j Circuit Breaker

## 이벤트 흐름

### 발행 이벤트 → Kafka 토픽

| 이벤트 | 토픽 | 소비자 |
|--------|------|--------|
| PaymentCompletedEvent | `pickme.payment.events` | Order (주문 확정), Notification (알림), Settlement (매출 집계) |
| PaymentFailedEvent | `pickme.payment.events` | Order (주문 취소 보상) |
| RefundCompletedEvent | `pickme.payment.events` | Settlement (환불 반영), Notification (알림) |

### 구독 이벤트

| 이벤트 | 발행자 | 토픽 | 처리 |
|--------|--------|------|------|
| OrderPlacedEvent | Order | `pickme.order.events` | 결제 처리 시작 (processNewPayment) |
| OrderRefundRequestedEvent | Order | `pickme.order.events` | 환불 처리 |

## API

| Method | URI | 설명 |
|--------|-----|------|
| GET | `/api/v1/payments/{id}` | 결제 조회 |
| GET | `/api/v1/payments?orderId=` | 주문별 결제 조회 |

## 패키지 구조

```
pickme-payment/
├── api/              PaymentController, Request/Response DTO
├── application/      PaymentService, PaymentEventHandler, PaymentCommandAdapter
├── domain/
│   ├── model/        Payment, PaymentId, PaymentMethod, PaymentStatus, Money, PgResponse
│   ├── event/        PaymentCompletedEvent, PaymentFailedEvent, RefundCompletedEvent
│   ├── service/      PaymentProcessingService (도메인 순수)
│   └── repository/   PaymentRepository (Interface)
└── infrastructure/
    ├── persistence/  JPA Entity, Mapper, Repository 구현체
    ├── external/     MockPgPaymentAdapter (PG 연동 + Circuit Breaker)
    ├── messaging/    PaymentEventConsumer (OrderPlacedEvent/RefundEvent Temporal 시 스킵)
    └── config/       PaymentDomainConfig (Domain Service Bean 등록)
```

## Temporal 연동

`PaymentCommandAdapter`가 `PaymentCommandPort`를 구현. Activity 재시도 시 REQUESTED/PROCESSING 상태의 기존 결제도 성공으로 반환(멱등성).

| 메서드 | 설명 | 멱등성 키 |
|--------|------|----------|
| `processPayment(orderId, ordererId, amount, method)` | PG 결제 처리 | `temporal-payment:{orderId}` |
| `processRefund(orderId, refundAmount)` | PG 환불 처리 | `temporal-refund:{orderId}` |
