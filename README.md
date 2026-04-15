# pick-me

> MSA + DDD + EDA 아키텍처 기반 커머스 플랫폼

---

## 아키텍처 개요

```
┌─────────────────────────────────────────────────────────────┐
│                       API Gateway                           │
│                  (Spring Cloud Gateway)                     │
│              JWT 검증 + 서비스 라우팅                            │
└──────────┬──────────┬──────────┬──────────┬─────────────────┘
           │          │          │          │
     ┌─────▼───┐ ┌───▼────┐ ┌──▼───┐ ┌───▼─────┐
     │  Order  │ │Payment │ │Product│ │Inventory│  ...
     │ Module  │ │ Module │ │Module │ │ Module  │
     └────┬────┘ └───┬────┘ └──┬───┘ └────┬────┘
          │          │         │           │
          └──────────┴─────────┴───────────┘
                         │
                   ┌─────▼─────┐
                   │   Kafka   │    ← 모든 모듈 간 통신은 도메인 이벤트로만
                   │  (Event)  │
                   └─────┬─────┘
                         │
              ┌──────────┼──────────┐
              │          │          │
        ┌─────▼───┐ ┌───▼────┐ ┌──▼──────────┐
        │  Member │ │Partner │ │ Settlement  │
        │ Module  │ │Module  │ │   Module    │
        └─────────┘ └────────┘ └─────────────┘
```

**핵심 원칙:**
- **MSA**: 모듈 간 직접 import 금지 (ArchUnit CI 강제), Schema-per-Module DB 격리
- **EDA**: Transactional Outbox → Debezium CDC → Kafka, 멱등성 보장, DLQ 모니터링
- **DDD**: Rich Domain Model, 도메인 순수성 (Spring/JPA 무의존), Tell Don't Ask

---

## Bounded Context 맵

```
┌─────────┐    OrderPlacedEvent     ┌───────────┐
│  Order  │ ──────────────────────▶ │ Inventory │
│         │ ◀── InventoryReserved   │           │
│         │ ◀── InventoryShortage   └───────────┘
│         │
│         │    OrderPlacedEvent     ┌───────────┐
│         │ ──────────────────────▶ │  Payment  │
│         │ ◀── PaymentCompleted    │           │
│         │ ◀── PaymentFailed       └───────────┘
└─────────┘
     │ OrderPlaced/Confirmed/Cancelled
     ▼
┌──────────────┐  ┌──────────┐  ┌────────────┐
│ Notification │  │  Member  │  │ Settlement │
└──────────────┘  └──────────┘  └────────────┘
```

---

## 기술 스택

| 계층 | 기술 |
|------|------|
| Runtime | Java 21 (Virtual Threads) |
| Framework | Spring Boot 3.4.4 |
| DB | PostgreSQL 16 (Schema-per-Module) |
| Cache | Redis 7 (분산 락, 캐시, Rate Limiter) |
| Message Broker | Apache Kafka (KRaft) |
| Resilience | Resilience4j (Circuit Breaker) |
| Architecture Test | ArchUnit |
| Workflow Orchestration | Temporal 1.25.0 (Saga, 보상 트랜잭션) |
| CDC | Debezium 2.5 (PostgreSQL WAL → Kafka) |
| Monitoring | Micrometer + Prometheus + Grafana |
| Logging | Promtail + Loki |
| Tracing | Zipkin |
| Container | Docker + Docker Compose |
| Gateway | Spring Cloud Gateway |
| Config | Spring Cloud Config Server |
| Discovery | Spring Cloud Netflix Eureka |
| CI | GitHub Actions |
| AI Code Assistant | Claude Code (Harness: Commands, Rules, Hooks) |

---

## 모듈 구조

[우아한형제들 멀티모듈 설계](https://techblog.woowahan.com/2637/)를 참고하여 역할과 책임에 따라 4개 계층으로 분류합니다.

```
pick-me/
├── application/          ← 어플리케이션 모듈 (실행/배포 단위)
├── domain-modules/       ← 도메인 모듈 (순수 도메인 비즈니스)
├── independent/          ← 독립 모듈 (도메인을 모르는 기능 모듈)
└── common/               ← 공통 모듈 (프로젝트 전체 공유)
```

### 도메인 모듈 (`domain-modules/`)

도메인 비즈니스에 집중하며, 공통 모듈만 의존합니다.

| 모듈 | 역할 | Bounded Context | README |
|------|------|-----------------|--------|
| **pickme-order** | 주문 생성, 상태 관리, Saga 이벤트 | Order | [README](domain-modules/pickme-order/README.md) |
| **pickme-payment** | PG 결제, 환불, Domain Service | Payment | [README](domain-modules/pickme-payment/README.md) |
| **pickme-product** | 상품 CRUD, Redis 캐시 | Product | [README](domain-modules/pickme-product/README.md) |
| **pickme-inventory** | 재고 예약/확정/복원, 분산 락 | Inventory | [README](domain-modules/pickme-inventory/README.md) |
| **pickme-member** | 회원 가입, JWT 인증, 등급 관리 | Member | [README](domain-modules/pickme-member/README.md) |
| **pickme-partner** | 파트너 등록/승인, ACL Gateway | Partner | [README](domain-modules/pickme-partner/README.md) |
| **pickme-notification** | 알림 발송 (이메일/SMS/카카오) | Notification | [README](domain-modules/pickme-notification/README.md) |
| **pickme-settlement** | 정산 집계, ETL, Reconciliation | Settlement | [README](domain-modules/pickme-settlement/README.md) |

### 어플리케이션 모듈 (`application/`)

실행 가능한 어플리케이션으로, 도메인/공통/독립 모듈을 조합하여 독립 배포 단위를 구성합니다.

| 모듈 | 역할 | README |
|------|------|--------|
| **pickme-app** | Spring Boot 실행, Flyway, 프로필 | [README](application/pickme-app/README.md) |
| **pickme-orchestration** | Temporal 워크플로우 4종, Activity, Worker, 모니터링 | [README](application/pickme-orchestration/README.md) |
| **pickme-gateway** | API Gateway, JWT 필터, 라우팅 | [README](application/pickme-gateway/README.md) |
| **pickme-config-server** | Spring Cloud Config Server, 설정 중앙 관리 | [README](application/pickme-config-server/README.md) |
| **pickme-discovery** | Eureka Server, 서비스 디스커버리 | [README](application/pickme-discovery/README.md) |

### 공통 모듈 (`common/`)

프로젝트 전체에서 사용하는 공유 인프라입니다. 어떤 모듈에도 의존하지 않습니다.

| 모듈 | 역할 | README |
|------|------|--------|
| **pickme-common** | Outbox, 멱등성, 분산 락, Rate Limiter, 이벤트 공통 | [README](common/pickme-common/README.md) |
| **pickme-orchestration-api** | Temporal CommandPort 인터페이스, 오케스트레이션 DTO (순수 Java) | [README](common/pickme-orchestration-api/README.md) |

### 독립 모듈 (`independent/`)

시스템과 연관 있지만 도메인/어플리케이션을 모르는 기능 모듈입니다.

| 모듈 | 역할 | README |
|------|------|--------|
| **pickme-archunit** | 모듈 경계, 도메인 순수성, 네이밍 규칙 테스트 | [README](independent/pickme-archunit/README.md) |

---

## Saga 오케스트레이션 (주문 플로우)

**Temporal Orchestration** (기본 모드: `pickme.temporal.enabled=true`)

```
OrderService.createOrder()
  ├─ [TX 내] Order 저장 + OrderPlacedEvent → Outbox → Kafka (알림/정산 브로드캐스트)
  └─ [TX 커밋 후] Temporal OrderFulfillmentWorkflow 시작
       │
       ├─ Step 1: reserveInventory()    ← InventoryCommandPort
       ├─ Step 2: processPayment()      ← PaymentCommandPort (60s 타임아웃)
       ├─ Step 3: confirmOrder()        ← OrderCommandPort
       └─ Step 4: confirmInventory()    ← InventoryCommandPort
       
  보상 (자동):
    Step 2 실패 → restoreInventory + cancelOrder
    Step 3 실패 → refund + restoreInventory + cancelOrder
```

**Kafka Choreography** (폴백 모드: `pickme.temporal.enabled=false`)

```
  Order.place() → OrderPlacedEvent
    → [Inventory] stock.reserve() → InventoryReservedEvent
    → [Payment] processNewPayment() → PaymentCompletedEvent
      → [Order] order.completePayment() → OrderConfirmedEvent
```

Feature Flag 기반 Strangler Fig 패턴으로 Kafka↔Temporal 간 무중단 전환이 가능하다.

---

## CDC 마이그레이션: Outbox 폴링 → Debezium 로그 기반

이벤트 전파 메커니즘을 Outbox 폴링 Relay에서 Debezium CDC(로그 기반)로 전환했습니다.

```
Before (Polling):
  OutboxRelayScheduler → 500ms 폴링 → Kafka
  한계: ~20 events/sec, DB 부하, 다운타임 시 누락 위험

After (Debezium CDC):
  PostgreSQL WAL → Debezium 2.5 (pgoutput) → Outbox EventRouter SMT → Kafka
  개선: batch 2048, 밀리초 감지, DB 부하 없음, LSN 기반 자동 복구
```

| 항목 | Before (폴링) | After (Debezium CDC) |
|------|-------------|---------------------|
| 이벤트 감지 | 500ms 주기 폴링 | WAL 실시간 구독 |
| DB 부하 | 매 500ms SELECT 쿼리 | WAL 읽기만 (추가 쿼리 없음) |
| 다운타임 복구 | 누락 위험 | LSN 기반 자동 재개 |
| DELETE 감지 | 불가 | 가능 |
| 스키마 오버헤드 | published, retry_count 등 | 제거 (V16 마이그레이션) |

**전환 전략**: 병렬 운영(`pickme.outbox.relay.enabled: true`) → 커트오버(`false`) → V16 컬럼 정리

> 상세 기술 아티클: [CDC — Outbox 폴링에서 Debezium 로그 기반으로](tech-blog/cdc.md)

---

## Temporal 마이그레이션: Kafka Choreography → Temporal Orchestration

Saga 오케스트레이션을 Kafka 이벤트 코레오그래피에서 Temporal 워크플로우로 전환했습니다.

```
Before (Choreography):
  OrderPlacedEvent → 6개 Kafka Consumer에 사가 로직 분산
  한계: 상태 비가시성, 좀비 주문, 타임아웃 없음, 보상 순서 미보장

After (Temporal Orchestration):
  OrderFulfillmentWorkflow → 4단계 명시적 사가 + 자동 보상
  개선: 단일 워크플로우에 로직 응집, 30분 타임아웃, 관리자 취소 Signal
```

| 항목 | Before (Kafka) | After (Temporal) |
|------|---------------|-----------------|
| 사가 상태 조회 | 여러 서비스 로그 추적 | Temporal UI + REST API |
| 타임아웃 | 없음 (3AM 배치 탐지) | 워크플로우 30분 자동 만료 |
| 보상 트랜잭션 | 이벤트 기반 암묵적 | 코드 기반 명시적 |
| 관리자 개입 | 불가 | @SignalMethod cancelByAdmin |
| Kafka 역할 | 사가 + 브로드캐스트 | 브로드캐스트만 (알림, 정산, CQRS) |

**전환 전략**: Feature Flag(`pickme.temporal.enabled`) 기반 Strangler Fig 패턴 — Shadow Mode → Live → Kafka 소비자 삭제

> 상세 기술 아티클: [Temporal 마이그레이션 여정](tech-blog/temporal.md)

---

## 인프라 구성

```
┌─────────────────────────────────────────────────────────────────┐
│                     Docker Compose Stack                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  PostgreSQL 16         Redis 7            Kafka (KRaft)          │
│  ┌──────────────┐     ┌──────────┐       ┌──────────────┐      │
│  │wal_level=    │     │분산 락    │       │pickme.order  │      │
│  │  logical     │     │캐시      │       │pickme.payment│      │
│  │Schema-per-   │     │Rate Limit│       │  ... events  │      │
│  │  Module      │     └──────────┘       └──────┬───────┘      │
│  └──────┬───────┘                                │               │
│         │                                        │               │
│         │  WAL                          ┌────────▼───────┐      │
│         └────────────────────────────── │ Kafka Connect   │      │
│                                         │ (Debezium 2.5)  │      │
│                                         └─────────────────┘      │
│                                                                   │
│  Temporal 1.25.0 (워크플로우 오케스트레이션)                        │
│  Temporal UI (워크플로우 가시성)                                   │
│                                                                   │
│  Prometheus ──▶ Grafana        Promtail ──▶ Loki                │
│  Zipkin (분산 트레이싱)         Kafka UI (토픽 모니터링)            │
└─────────────────────────────────────────────────────────────────┘
```

### Docker Compose 파일

| 파일 | 용도 |
|------|------|
| `docker-compose.infra.yml` | 인프라만 실행 (로컬 개발) |
| `docker-compose.yml` | 인프라 + 앱 전체 실행 |
| `docker-compose.msa.yml` | MSA 모드 (서비스 분리 배포) |

---

## 실행 방법

```bash
# 인프라만 실행 (로컬 개발)
docker compose -f docker-compose.infra.yml up -d

# 전체 실행 (인프라 + 앱)
docker compose up -d --build

# MSA 모드 실행 (서비스 분리 배포)
docker compose -f docker-compose.msa.yml up -d --build

# 빌드 + 테스트
./gradlew clean build
```

| 서비스 | URL |
|--------|-----|
| Application | http://localhost:8080 |
| API Gateway | http://localhost:8060 |
| Config Server | http://localhost:8888 |
| Eureka Dashboard | http://localhost:8761 |
| Temporal UI | http://localhost:8233 |
| Kafka UI | http://localhost:8089 |
| Kafka Connect (Debezium) | http://localhost:8083 |
| Zipkin | http://localhost:9411 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |

---

## DDD 전술적 패턴 적용 현황

| 패턴 | 현황 |
|------|------|
| Entity (Rich Model) | 8/8 Aggregate에 비즈니스 메서드 캡슐화 |
| Value Object | 모든 VO 불변 + equals/hashCode + 생성자 검증 |
| Aggregate Root | private 생성자 + factory method + 상태 전이 가드 |
| Repository (Port) | domain에 Interface, infrastructure에 구현체 |
| Factory | register, place, request, create + reconstitute |
| Domain Service | PaymentProcessingService (도메인 순수) |
| Domain Event | Aggregate 내부 발행, DomainEventPublisher 단일 컴포넌트 |
| 도메인 순수성 | ArchUnit 강제 — Spring/JPA 어노테이션 누출 0건 |

---

## Claude Code 하네스 (AI 아키텍처 가드레일)

ArchUnit이 CI 빌드 시점에 아키텍처를 강제한다면, Claude Code 하네스는 **개발 시점에 MSA/EDA/DDD 규칙을 가이드**한다.

```
.claude/
├── commands/                          ← 슬래시 명령으로 호출하는 워크플로우 템플릿
│   ├── analyze-domain.md                 /analyze-domain {모듈}
│   ├── check-architecture.md             /check-architecture
│   ├── review-code.md                    /review-code {대상}
│   └── write-acceptance-test.md          /write-acceptance-test {시나리오}
├── rules/                             ← 파일 패턴 매칭 시 자동 주입되는 계층별 규칙
│   ├── domain-layer.md                   **/domain/**/*.java
│   ├── application-layer.md              **/application/**/*.java
│   ├── infrastructure-layer.md           **/infrastructure/**/*.java
│   └── test-patterns.md                  **/src/test/**/*.java
└── settings.json                      ← Hooks (PreToolUse, PostToolUse, Stop)
CLAUDE.md                              ← 프로젝트 개요 (매 세션 자동 로드)
```

### 3계층 가드레일

| 계층 | 파일 | 강제성 | 적용 시점 |
|------|------|--------|-----------|
| **CLAUDE.md** | 프로젝트 개요 | 권고 (읽기만) | 매 세션 자동 로드 |
| **Rules** | 계층별 작성 규칙 | 조건부 주입 | 매칭 파일 편집 시만 로드 |
| **Hooks** | 자동 검증/알림 | **강제 실행** | 도구 실행 전/후, 작업 완료 시 |

### Commands

| 명령 | 용도 |
|------|------|
| `/analyze-domain order` | 특정 도메인 모듈의 구조·DDD 전술·EDA 패턴을 3단계 Deep Dive 분석 |
| `/check-architecture` | ArchUnit 38개 규칙 실행 + MSA 격리 + EDA 정합성 + 부채 현황 종합 점검 |
| `/review-code {대상}` | MSA 8항목 + EDA 12항목 + DDD 16항목 = **36개 체크리스트** 기반 코드 리뷰 |
| `/write-acceptance-test {시나리오}` | BDD 인수 테스트 생성 (상태전이·Outbox·멱등성·이벤트핸들러·분산락 5계층 검증) |

### Rules × ArchUnit 연동

| Rule 파일 | 편집 대상 | 연동하는 ArchUnit 규칙 |
|-----------|----------|----------------------|
| `domain-layer.md` | `domain/**/*.java` | DomainPurityTest (4), DddTacticalPatternTest (5) |
| `application-layer.md` | `application/**/*.java` | IntraDomainLayeringTest, NamingConventionTest |
| `infrastructure-layer.md` | `infrastructure/**/*.java` | IntraDomainLayeringTest, ModuleBoundaryTest |
| `test-patterns.md` | `src/test/**/*.java` | BDD 컨벤션, Outbox/멱등성 검증 가이드 |

ArchUnit은 **위반을 사후 차단**하고, Rules는 **위반을 사전 방지**하는 상호 보완 관계이다.
