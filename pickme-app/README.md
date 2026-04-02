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

## 프로필

| 프로필 | 용도 | DB |
|--------|------|-----|
| `local` | 로컬 개발 (IDE) | H2 In-Memory |
| `docker` | Docker Compose | PostgreSQL |
| `prod` | 프로덕션 | 환경 변수 |
| `timeout` | Timeout 중앙 관리 | (모든 프로필에서 include) |

## 배치

- `ConsistencyCheckBatch`: 매일 03:00 — 좀비 주문 감지 + 주문-결제 정합성
- `ConsistencyFixController`: POST /api/v1/admin/consistency/{orderId}/fix — 수동 보정
