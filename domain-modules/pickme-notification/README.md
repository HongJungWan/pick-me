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

## 이벤트 흐름

### 발행 이벤트

없음 — Notification은 **Sink 모듈** (이벤트 소비만 수행, 발행하지 않음).

### 구독 토픽

| 토픽 | 소비 이벤트 | 알림 유형 |
|------|-----------|----------|
| `pickme.order.events` | OrderPlaced, OrderShipped, OrderDelivered | 고객 알림 |
| `pickme.payment.events` | PaymentCompleted | 고객 알림 |
| `pickme.member.events` | MemberRegistered | 가입 환영 |
| `pickme.inventory.events` | InventoryShortage | 운영 알림 |
| `pickme.settlement.events` | SettlementCompleted | 파트너 알림 |

### 알림 채널

| 채널 | 용도 |
|------|------|
| EMAIL | 주문 확인, 가입 환영, 정산 완료 |
| SMS | 배송 시작/완료 |
| KAKAO | 주문 접수, 결제 완료 |

## 패키지 구조

```
pickme-notification/
├── api/              (현재 외부 API 없음)
├── application/      NotificationEventHandler
├── domain/
│   ├── model/        Notification, NotificationId, NotificationChannel, SendStatus
│   ├── event/        (발행 이벤트 없음)
│   └── repository/   NotificationRepository (Interface)
└── infrastructure/
    ├── persistence/  JPA Entity, Mapper, Repository 구현체
    └── messaging/    NotificationEventConsumer (5개 토픽 구독)
```
