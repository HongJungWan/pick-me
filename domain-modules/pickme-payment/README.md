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

## API

| Method | URI | 설명 |
|--------|-----|------|
| GET | `/api/v1/payments/{id}` | 결제 조회 |
| GET | `/api/v1/payments?orderId=` | 주문별 결제 조회 |
