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

## 구독 이벤트

| 이벤트 | 발행자 | 처리 |
|--------|--------|------|
| PaymentCompletedEvent | Payment | 매출 누적 |
| RefundCompletedEvent | Payment | 환불 반영 |
| PartnerApprovedEvent | Partner | 파트너 스냅샷 upsert |
