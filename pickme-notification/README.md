# pickme-notification (Notification Context)

> 알림 발송 — 이벤트 소비 + 용도별 Factory Method

## Aggregate — `Notification`

7개 용도별 팩토리 메서드로 알림을 생성한다. 템플릿/메시지 로직이 Aggregate 내부에 캡슐화되어 있어, Handler는 팩토리 호출 + save만 수행한다.

| 팩토리 메서드 | 트리거 이벤트 | 알림 내용 |
|--------------|-------------|-----------|
| `forOrderPlaced()` | OrderPlacedEvent | 주문 접수 알림 |
| `forPaymentCompleted()` | PaymentCompletedEvent | 결제 완료 알림 |
| `forMemberRegistered()` | MemberRegisteredEvent | 가입 환영 알림 |
| `forOrderShipped()` | OrderShippedEvent | 배송 시작 알림 |
| `forOrderDelivered()` | OrderDeliveredEvent | 배송 완료 알림 |
| `forInventoryShortage()` | InventoryShortageEvent | 재고 부족 운영 알림 |
| `forSettlementCompleted()` | SettlementCompletedEvent | 정산 완료 알림 |

## 상태 전이 가드

- `send()`: PENDING → SENT (PENDING이 아니면 예외)
- `markFailed()`: PENDING → FAILED (PENDING이 아니면 예외)

## 구독 토픽

`pickme.order.events`, `pickme.payment.events`, `pickme.member.events`, `pickme.inventory.events`, `pickme.settlement.events`
