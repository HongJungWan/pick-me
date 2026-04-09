# pickme-settlement (Settlement Context)

> 정산 집계, ETL Aggregate Table, Reconciliation 배치

## Aggregate Root — `Settlement`

| 메서드 | 설명 | 발행 이벤트 |
|--------|------|------------|
| `create()` | 정산 생성 (Factory) | - |
| `recordSale(amount)` | 매출 기록 (CALCULATING 상태만) | - |
| `recordRefund(amount)` | 환불 기록 (CALCULATING 상태만) | - |
| `confirm()` | 정산 확인 | - |
| `requestTransfer()` | 이체 요청 | - |
| `complete()` | 정산 완료 | SettlementCompletedEvent |
| `isReconciled()` | 정합성 검증 | - |

## 상태 전이

```
CALCULATING → CONFIRMED → TRANSFER_REQUESTED → COMPLETED
```

## CQRS Read Model (ETL Aggregate Table)

- `settlement_schema.daily_sales_aggregate` — 일별 파트너별 매출/환불/순매출 집계
- `settlement_schema.partner_snapshot` — 파트너 계약 정보 스냅샷 (수수료율 등)

## Reconciliation 배치

- 매일 02:00 실행 (`@Scheduled`)
- `SalesSnapshotEntity.isReconciled()`: 도메인에 캡슐화된 정합성 검증

## 이벤트 흐름

### 발행 이벤트 → Kafka 토픽

| 이벤트 | 토픽 | 소비자 |
|--------|------|--------|
| SettlementCompletedEvent | `pickme.settlement.events` | Notification (정산 완료 알림) |

### 구독 이벤트

| 이벤트 | 발행자 | 토픽 | 처리 |
|--------|--------|------|------|
| PaymentCompletedEvent | Payment | `pickme.payment.events` | 매출 누적 (recordSale) |
| RefundCompletedEvent | Payment | `pickme.payment.events` | 환불 반영 (recordRefund) |
| PartnerApprovedEvent | Partner | `pickme.partner.events` | 파트너 스냅샷 upsert |

## 패키지 구조

```
pickme-settlement/
├── api/              SettlementController, Request/Response DTO
├── application/      SettlementService, SettlementEventHandler, SettlementCommandAdapter
├── domain/
│   ├── model/        Settlement, SettlementId, SettlementStatus, SettlementPeriod
│   ├── event/        SettlementCompletedEvent
│   └── repository/   SettlementRepository (Interface)
└── infrastructure/
    ├── persistence/  JPA Entity, Mapper, Repository 구현체
    ├── messaging/    SettlementEventConsumer
    └── snapshot/     SalesSnapshotEntity, PartnerSnapshotEntity (ETL Aggregate)
```

## Temporal 연동

`SettlementCommandAdapter`가 `SettlementCommandPort`를 구현하여 `SettlementReconciliationWorkflow` Activity에서 호출된다.

| 메서드 | 설명 |
|--------|------|
| `fetchDailySnapshots(date)` | 일일 정산 스냅샷 조회 (읽기 전용) |
| `reconcilePartner(partnerId, date)` | 파트너별 정산 검증 (읽기 전용) |

기존 `SettlementService.reconciliationBatch()`의 @Scheduled cron 로직을 Temporal 워크플로우로 전환. 파트너별 개별 재시도와 불일치 Slack 알림을 지원한다.
