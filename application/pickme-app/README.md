# pickme-app (실행 모듈)

> Spring Boot 실행 진입점, Flyway 마이그레이션, 프로필 관리

## 역할

모든 도메인 모듈을 통합하여 **단일 Spring Boot JAR**로 실행한다 (Phase 1: Modular Monolith).

## Flyway 마이그레이션

| 버전 | 내용 |
|------|------|
| V1 | 8개 Schema-per-Module 생성 |
| V2 | 모듈별 outbox_events + processed_events (8스키마 × 2테이블) |
| V3 | product_schema: products, product_options |
| V4 | inventory_schema: stocks, stock_history |
| V5 | member_schema: members |
| V6 | order_schema: orders, order_lines |
| V7 | order_schema: product_snapshot, member_snapshot (CQRS) |
| V8 | payment_schema: payments |
| V9 | notification_schema: notifications |
| V10 | partner_schema: partners |
| V11 | settlement_schema: daily_sales_aggregate |
| V12 | dead_letter_events |
| V13 | orders 테이블 월별 Range Partitioning |
| V14 | settlement_schema: partner_snapshot |
| V15 | Debezium Heartbeat 테이블 생성 (WAL 슬롯 LSN 전진용) |
| V16 | Outbox 폴링 컬럼 제거 (published, published_at, retry_count) — 8개 스키마 |

## 프로필

| 프로필 | 용도 | DB |
|--------|------|-----|
| `local` | 로컬 개발 (IDE) | H2 In-Memory |
| `docker` | Docker Compose | PostgreSQL |
| `prod` | 프로덕션 | 환경 변수 |
| `timeout` | Timeout 중앙 관리 | (모든 프로필에서 include) |

## 배치

| 클래스 | 스케줄 | 역할 |
|--------|--------|------|
| `ConsistencyCheckBatch` | 매일 03:00 | 좀비 주문 감지 (PLACED/PAYMENT_PENDING > 2시간) + 주문-결제 정합성 검증 |
| `ConsistencyFixController` | 수동 | POST /api/v1/admin/consistency/{orderId}/fix — 3가지 시나리오 보정 |

보정 시나리오:
1. PAID 상태인데 Payment 없음 → 주문 취소
2. CANCELLED 상태인데 Payment 존재 → 환불 처리
3. 좀비 주문 (2시간 초과 미결제) → 주문 취소
