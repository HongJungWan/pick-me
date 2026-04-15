# pick-me

MSA + DDD + EDA 커머스 플랫폼. Java 21, Spring Boot 3.4.4, PostgreSQL, Kafka, Redis, Temporal.

## Quick Start

```bash
# 인프라 (PostgreSQL, Kafka, Redis, Zipkin, Prometheus, Grafana)
docker compose -f docker-compose.infra.yml up -d

# 전체 빌드
./gradlew clean build

# 특정 도메인 모듈 컴파일
./gradlew :domain-modules:pickme-order:compileJava

# ArchUnit 아키텍처 테스트 (38개 규칙)
./gradlew :independent:pickme-archunit:test

# 앱 실행
./gradlew :application:pickme-app:bootRun
```

## 모듈 구조

```
pick-me/
├── application/          # 실행/배포 단위 (app, orchestration, gateway, config, discovery)
├── domain-modules/       # 8개 도메인 (order, payment, product, inventory, member, partner, notification, settlement)
├── common/               # 공유 인프라 (pickme-common, pickme-orchestration-api)
├── independent/          # 독립 모듈 (pickme-archunit)
└── infra/                # Docker, Debezium, Prometheus 설정
```

## 아키텍처 3원칙 (MSA + EDA + DDD)

### MSA
- 모듈 간 직접 import 금지 — Kafka 도메인 이벤트로만 통신 (ArchUnit `ModuleBoundaryTest` 강제)
- Schema-per-Module — 8개 독립 PostgreSQL 스키마, Cross-module JOIN 불가
- 각 모듈이 독립적인 `outbox_events` + `processed_events` 테이블 보유
- CQRS Read Model — `infrastructure/snapshot/` 에 이벤트 기반 스냅샷 (ProductSnapshot, MemberSnapshot, PartnerSnapshot, SalesSnapshot)
- Redis 분산 락 — `@DistributedLock` (Redisson RLock), 단일 JVM 의존성 제거

### EDA
- Transactional Outbox — 비즈니스 로직 + `OutboxEvent` INSERT 같은 @Transactional
- `OutboxRelayScheduler` (500ms 폴링) → Kafka 토픽 `pickme.{aggregateType}.events`
- 멱등성 — `IdempotencyFilter` + `processed_events` 테이블 (eventId 기반 중복 제거)
- DLQ — `pickme.dead-letter` 토픽 → `DeadLetterConsumer` → `dead_letter_events` 테이블 + Slack 알림
- 이벤트 카탈로그 — `EVENT-CATALOG.md` (Publisher/Consumer/Payload 계약)

### DDD
- Bounded Context 8개, Aggregate Root는 `DomainEventProvider` 구현
- domain 패키지 순수성 — Spring/JPA 어노테이션 금지 (`DomainPurityTest` 강제)
- Value Object — 모든 도메인 모듈에서 Primitive 대신 VO 사용 (Money, OrderId, Email, Address 등)
- 상태 전이 캡슐화 — `Order.cancel()`, `Partner.approve()` 등 비즈니스 메서드 내부에서만 상태 변경
- ACL — Partner 모듈의 `DeliveryGateway`, `NotificationGateway`가 외부 API → 내부 도메인 모델 변환
- Tell, Don't Ask — getter로 꺼내 외부 판단 금지

## 도메인 모듈 내부 계층 (5-Layer)

```
api/            → Controller, Request/Response DTO
application/    → Service, EventHandler, CommandAdapter
domain/model/   → Aggregate Root, Value Object, Enum
domain/event/   → DomainEvent 구현체
domain/repository/ → Repository 포트 (인터페이스)
infrastructure/persistence/ → JPA Entity, RepositoryImpl, Mapper
infrastructure/messaging/   → Kafka Consumer
infrastructure/snapshot/    → CQRS Read Model (스냅샷 엔티티)
infrastructure/external/    → ACL Gateway (외부 API 어댑터)
```

## 이벤트 흐름

```
Order → OrderPlacedEvent → Inventory, Payment
Inventory → InventoryReserved/Shortage → Order
Payment → PaymentCompleted/Failed → Order
Order → OrderConfirmed/Cancelled → Notification, Member, Settlement
Partner → PartnerApproved → Settlement
Payment → RefundCompleted → Order, Notification, Settlement
```

## ArchUnit 규칙 (8개 테스트 클래스, 38개 규칙)

- `DomainPurityTest` (4) — 도메인→인프라/API/Spring/JPA 의존 금지
- `ModuleBoundaryTest` (1) — 8개 도메인 모듈 간 직접 의존 금지
- `CrossDomainCommunicationTest` (2) — 상위↔하위 도메인 직접 의존 금지
- `DddTacticalPatternTest` (5) — setter 금지, private 생성자, VO final, 이벤트 위치, Lombok 제한
- `SharedModuleIsolationTest` (2) — common/infra 모듈 → 도메인 모듈 역방향 의존 금지
- `NamingConventionTest` (6) — Controller/Service/EventHandler/Config/Gateway/Enum 네이밍
- `IntraDomainLayeringTest` (11) — 8개 모듈 레이어링 + Repository 위치 3개
- `TemporalIsolationTest` (7) — Temporal SDK 격리, Activity/Workflow 위치, 비결정성 API 금지

## 아키텍처 부채 현황

- 총 67개 frozen 위반 (최초 137개 대비 51% 감소)
- 주요: Application→API DTO 의존 57건 (Command 패턴 도입 시 해소)
- 상세: `docs/archunit-debt-backlog.md`

## 테스트 컨벤션

- BDD: `// given` / `// when` / `// then` 주석 구분
- 메서드명: 한국어 snake_case (예: `주문_생성시_재고가_차감된다`)
- Assertion: AssertJ fluent style
- 통합 테스트: @SpringBootTest + TestContainers (PostgreSQL, Kafka, Redis)
- 비동기 검증: Awaitility (`await().atMost(5, SECONDS).until(...)`)
- Outbox 검증 필수: 도메인 이벤트 발행 시 outbox_events 테이블 확인
- 아키텍처 테스트: `./gradlew :independent:pickme-archunit:test`

## 주요 참조 문서

- `EVENT-CATALOG.md` — 전체 도메인 이벤트 카탈로그 (Publisher/Consumer/Payload)
- `PRD.md` — 제품 요구사항
- `docs/archunit-debt-backlog.md` — 아키텍처 부채 백로그 (카테고리 A~F)
