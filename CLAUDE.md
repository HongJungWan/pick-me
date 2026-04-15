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

# ArchUnit 아키텍처 테스트
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

## 아키텍처 규칙

- **모듈 간 직접 import 금지** — Kafka 이벤트로만 통신 (ArchUnit CI 강제)
- **도메인 순수성** — domain 패키지는 Spring/JPA 어노테이션 금지
- **의존 방향** — domain <- application <- api, domain <- infrastructure
- **Transactional Outbox** — 도메인 이벤트는 Outbox 테이블 → Debezium CDC → Kafka
- **Schema-per-Module** — 모듈별 독립 PostgreSQL 스키마

## Bounded Context 이벤트 흐름

Order -> (OrderPlacedEvent) -> Inventory, Payment
Inventory -> (InventoryReserved/Shortage) -> Order
Payment -> (PaymentCompleted/Failed) -> Order
Order -> (OrderConfirmed/Cancelled) -> Notification, Member, Settlement

## 테스트 컨벤션

- BDD: Given-When-Then 주석 구분
- 메서드명: 한국어 snake_case (예: `주문_생성시_재고가_차감된다`)
- Assertion: AssertJ fluent style
- 통합 테스트: @SpringBootTest + TestContainers
- 아키텍처 테스트: ArchUnit (독립 모듈)

## 주요 참조 문서

- `EVENT-CATALOG.md` — 전체 도메인 이벤트 카탈로그
- `PRD.md` — 제품 요구사항
- `docs/archunit-debt-backlog.md` — 아키텍처 부채 백로그
