# pick-me 커머스 플랫폼 PRD (Product Requirements Document)

> **프로젝트명**: pick-me  
> **버전**: v1.0.0-draft  
> **작성일**: 2026-04-02  
> **아키텍처**: Modular Monolith → MSA (DDD + EDA + CQRS)  
> **기술 스택**: Spring Boot 3.x, Java 21, PostgreSQL, Redis, Apache Kafka  

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [아키텍처 원칙](#2-아키텍처-원칙)
3. [Bounded Context 정의](#3-bounded-context-정의)
4. [도메인 이벤트 카탈로그](#4-도메인-이벤트-카탈로그)
5. [데이터베이스 스키마 전략](#5-데이터베이스-스키마-전략)
6. [트래픽/성능 전략](#6-트래픽성능-전략)
7. [가용성 패턴](#7-가용성-패턴)
8. [팀 컨벤션 및 개발 표준](#8-팀-컨벤션-및-개발-표준)
9. [단계별 구현 계획 (Issue 단위)](#9-단계별-구현-계획-issue-단위)
10. [프로젝트 구조](#10-프로젝트-구조)
11. [기술 부채 및 리스크 관리](#11-기술-부채-및-리스크-관리)

---

## 1. 프로젝트 개요

### 1.1 비전

한국 커머스 시장을 위한 확장 가능한 주문/결제/정산 플랫폼. Modular Monolith로 시작하여 트래픽 증가에 따라 MSA로 자연스럽게 전환 가능한 아키텍처를 설계한다.

### 1.2 핵심 목표

- **Phase 1**: Modular Monolith로 빠른 MVP 출시 (단일 배포 단위, 모듈 간 경계 엄격)
- **Phase 2**: 트래픽 병목 분석 및 성능 최적화, 모니터링 체계 확립
- **Phase 3**: 병목 모듈 우선 분리를 통한 점진적 MSA 전환 (Kafka 기반 비동기 통신)

### 1.3 모놀리식 한계와 MSA 전환 근거

모놀리식 아키텍처는 초기 개발 속도와 단순함이라는 장점이 있지만, 서비스 성장에 따라 구조적 한계가 드러난다.

**모놀리식의 탄생 배경과 전제**: 단일 프로세스, 단일 DB, 단일 배포 단위라는 전제 위에서 동작하며, 트래픽이 낮고 팀 규모가 작을 때 최적의 선택이다. 하지만 이 전제가 깨지는 순간 구조적 문제가 연쇄적으로 발생한다.

| 구분 | 모놀리식 한계 | MSA 해결 |
|------|-------------|---------|
| **배포** | 전체 재배포 필요, 배포 주기 길어짐 | 모듈별 독립 배포 |
| **확장** | 전체 스케일링만 가능 (비용 폭발) | 병목 모듈만 선택적 스케일링 |
| **장애** | 단일 모듈 장애 → 전체 장애 (Noisy Neighbor) | 장애 격리, Circuit Breaker |
| **DB** | 단일 DB 커넥션 풀 경합, 지수적 병목 증가 | Schema/DB 분리로 독립 확장 |
| **팀** | 코드 충돌, 모듈 간 숨은 의존성 증가 | 팀별 독립 개발/배포 |
| **트래픽** | 읽기 집중 기능과 쓰기 집중 기능이 동일 자원 경합 | 트래픽 특성 기반 분리 |

**트래픽 흐름 관점에서의 한계**: 모놀리식에서는 모든 요청이 동일한 프로세스를 통과하며, 트래픽 증가 시 가장 느린 경로(주로 DB 접근)가 전체 시스템의 병목이 된다. 상품 조회(Read Heavy)와 주문 처리(Write Heavy)가 같은 DB 커넥션 풀을 공유하면, 한쪽의 부하가 다른 쪽의 응답 시간을 지수적으로 악화시킨다.

**스케일 아웃의 구조적 한계**: 모놀리식에서 스케일 아웃하면 불필요한 모듈까지 함께 복제된다. 재고 차감이 병목인데 알림/정산 모듈까지 복제하는 것은 자원 낭비이며, 무엇보다 상태를 공유하는 DB가 단일이므로 DB 자체가 최종 병목점이 된다.

**전환 기준**: MSA 전환은 "기술적 욕심"이 아니라 "구조적 필요"에 의해 결정한다.
- 평균 TPS 500 이상 또는 피크 TPS 5,000 이상 시 병목 모듈 분리 시작
- 배포 충돌이 주 3회 이상 발생 시
- 단일 모듈 장애로 전체 서비스 장애가 월 1회 이상 발생 시

> **MSA는 '완성형'이 아니라 '진화형'이다.** Phase 1부터 모듈 경계를 엄격히 지키면서 Modular Monolith로 시작하고, 실제 병목이 관측되는 시점에 해당 모듈을 분리한다.

### 1.4 기술 스택 상세

| 계층 | 기술 | 용도 |
|------|------|------|
| Runtime | Java 21 (Virtual Threads) | Loom 기반 경량 스레드로 I/O 처리량 극대화 |
| Framework | Spring Boot 3.x | Modular Monolith 기반, Spring Modulith 활용 |
| DB | PostgreSQL 16 | Schema-per-Module, JSONB 활용 |
| Cache | Redis 7 Cluster | 분산 캐시, 분산 락 (Redisson), Rate Limiter |
| Message Broker | Apache Kafka (AWS MSK) | Outbox Relay, 모듈 간 비동기 통신, 이벤트 스트리밍 |
| Search (선택) | OpenSearch | 상품 검색 CQRS Read Model |
| Monitoring | Micrometer + Prometheus + Grafana | 메트릭, 대시보드, 알림 |
| Tracing | Zipkin / Jaeger | 분산 트레이싱, 이벤트 흐름 추적 |
| Resilience | Resilience4j | Circuit Breaker, Rate Limiter, Bulkhead |
| Architecture Test | ArchUnit | 모듈 경계 + 도메인 순수성 CI 강제 |
| DB Migration | Flyway | Schema-per-Module 마이그레이션 관리 |
| Container | Docker + Docker Compose | 로컬 개발/CI 환경 통합, 인프라 일관성 보장 |

---

## 2. 아키텍처 원칙

### 2.1 MSA 원칙 (Modular Monolith 단계부터 적용)

#### 2.1.1 모듈 간 직접 참조 금지

모듈 간 직접 import, 메서드 호출을 완전 금지한다. 모든 모듈 간 통신은 도메인 이벤트로만 수행된다.

**ArchUnit 검증 대상** (CI에서 강제):
- `*.order.internal.*` → `*.payment.internal.*` import 금지
- `*.domain.*` → `*.infrastructure.*` import 금지 (도메인 순수성)
- `*.domain.*` → `*.api.*` import 금지
- `*.domain.*` → `org.springframework..*` import 금지 (프레임워크 독립)

서비스 분리 시 모듈을 그대로 떼어내면 독립 서비스가 된다. 이 원칙이 지켜지지 않으면 분리 비용이 기하급수적으로 증가한다.

#### 2.1.2 Schema-per-Module DB 격리

```
PostgreSQL Database: pickme_db
├── order_schema       (주문 모듈 전용)
├── payment_schema     (결제 모듈 전용)
├── product_schema     (상품 모듈 전용)
├── inventory_schema   (재고 모듈 전용)
├── member_schema      (회원/인증 모듈 전용)
├── partner_schema     (파트너 모듈 전용)
├── notification_schema (알림 모듈 전용)
└── settlement_schema  (정산 모듈 전용)
```

- **Cross-Module JOIN 절대 금지**: 각 모듈은 자기 스키마만 접근
- 모듈별 독립 DataSource 설정 (Spring의 AbstractRoutingDataSource 또는 모듈별 EntityManagerFactory)
- Phase 3 MSA 전환 시 스키마 → 독립 DB 인스턴스로 자연스럽게 분리

#### 2.1.3 모듈별 독립 Outbox 테이블

각 모듈 스키마 내에 `outbox_events` 테이블을 보유한다. 서비스 분리 시 Outbox 테이블을 그대로 가져간다.

```sql
CREATE TABLE {module}_schema.outbox_events (
    id              BIGSERIAL PRIMARY KEY,
    event_id        UUID NOT NULL UNIQUE,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(200) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    published       BOOLEAN NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMP,
    retry_count     INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_unpublished 
    ON {module}_schema.outbox_events (published, created_at) 
    WHERE published = FALSE;
```

#### 2.1.4 CQRS Read Model (이벤트 기반 스냅샷)

외부 모듈 데이터가 필요한 경우 → 해당 모듈의 도메인 이벤트를 구독하여 **로컬 Read Model(스냅샷)** 에 저장한다.

- 예: 주문 모듈이 상품명이 필요하면 → `ProductRegisteredEvent` 구독 → `order_schema.product_snapshot` 테이블에 저장
- Read Model은 **Eventually Consistent** (최종 일관성)
- Cross-Module JOIN 대신 로컬 스냅샷 조회로 외부 데이터 의존성 완전 제거

**CQRS 필요성**: 읽기와 쓰기의 트래픽 비율이 극단적으로 다를 때 (상품 조회 vs 상품 등록 = 100:1), 단일 모델로는 양쪽을 최적화할 수 없다. CQRS는 읽기 전용 모델을 분리하여 각각 독립적으로 스케일링할 수 있게 한다. 아키텍처 수준에서 접근하면, Command Side는 정합성과 비즈니스 규칙에, Query Side는 조회 성능과 데이터 조합에 최적화된다.

#### 2.1.5 Redis 분산 락

단일 JVM 의존성을 제거하기 위해 Redisson 기반 분산 락을 사용한다. 멀티 인스턴스 배포 시에도 동시성 제어가 보장된다.

- **적용 대상**: 재고 차감, 주문 상태 전이, 결제 처리
- **Lock Key 네이밍**: `lock:{module}:{aggregate}:{id}` (예: `lock:inventory:stock:SKU-001`)
- **TTL**: 기본 5초, 최대 30초 (deadlock 방지)
- **waitTime**: 3초 (락 획득 대기 최대 시간, 초과 시 즉시 실패)

### 2.2 EDA 원칙

#### 2.2.1 모듈 간 통신 = 도메인 이벤트 Only

```
주문 모듈 → (OrderPlacedEvent) → Kafka Topic → 재고 모듈
재고 모듈 → (InventoryReservedEvent) → Kafka Topic → 결제 모듈
결제 모듈 → (PaymentCompletedEvent) → Kafka Topic → 주문 모듈
```

직접 메서드 호출, 동기 REST 호출 없음. 모듈 간 통신은 오직 도메인 이벤트로만 수행한다. 이벤트 기반 느슨한 결합으로 모듈 간 독립적인 배포, 확장, 장애 격리가 가능해진다.

#### 2.2.2 Transactional Outbox 패턴

비즈니스 로직과 이벤트 발행을 같은 트랜잭션으로 묶어 **Dual-Write 문제**를 해결한다.

```java
@Transactional
public void placeOrder(PlaceOrderCommand cmd) {
    // 1. 비즈니스 로직 (Order Aggregate 상태 변경)
    Order order = Order.place(cmd);
    orderRepository.save(order);
    
    // 2. 같은 트랜잭션에서 Outbox INSERT
    outboxRepository.save(OutboxEvent.from(order.getDomainEvents()));
    
    // → DB 트랜잭션 하나로 비즈니스 + 이벤트 발행 원자성 보장
    // → 애플리케이션 크래시 시에도 이벤트 유실 없음
}
```

만약 비즈니스 로직과 메시지 브로커 발행을 별도로 수행하면, DB 커밋 후 Kafka 발행 전 크래시 시 이벤트가 유실된다. Outbox 패턴은 이 문제를 근본적으로 해결한다.

#### 2.2.3 Outbox → Kafka Relay

**Phase 1 — Polling Publisher**:
- 스케줄러(매 500ms)가 `published = FALSE`인 Outbox 레코드를 조회
- Kafka Topic으로 발행 → `published = TRUE`로 업데이트
- 배치 단위 처리 (최대 10건/회)
- Kafka Producer의 `acks=all` + `enable.idempotence=true`로 발행 신뢰성 보장

**Phase 2+ 고도화 — Debezium CDC**:
- Outbox 테이블의 INSERT를 CDC(Change Data Capture)로 감지
- Kafka Connect를 통해 자동 발행 (Polling Publisher 대체)
- Polling 방식 대비 지연 시간 대폭 감소 (500ms → 수십ms)
- 애플리케이션 코드에서 Outbox Polling 로직 제거 가능

#### 2.2.4 멱등성 보장 (Idempotency)

각 모듈 스키마 내에 `processed_events` 테이블을 두어 중복 처리를 방지한다.

```sql
CREATE TABLE {module}_schema.processed_events (
    event_id     UUID PRIMARY KEY,
    event_type   VARCHAR(200) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

- 이벤트 수신 시 `event_id`로 중복 체크 → 이미 처리된 이벤트는 ACK(offset commit) 후 무시
- Kafka Consumer의 at-least-once delivery 특성 대응
- Consumer 재시작/리밸런싱으로 인한 중복 수신 대응

> **멱등성이란**: 같은 연산을 여러 번 수행해도 결과가 달라지지 않는 성질. 분산 환경에서는 네트워크 장애, Consumer 재시작 등으로 동일 이벤트가 2회 이상 전달될 수 있으므로, 모든 이벤트 핸들러는 반드시 멱등하게 설계해야 한다.

#### 2.2.5 DLQ (Dead Letter Queue)

- Kafka Consumer에서 최대 3회 재시도 후 실패 시 DLT(Dead Letter Topic)로 이동
- Spring Kafka의 `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` 활용
- DLT 메시지는 별도 모니터링 + 수동/자동 재처리 API 제공
- DLT 적재 시 Slack 알림 발송 (운영팀 즉시 인지)
- 실패 원인 분석 후 수동 재처리 또는 보상 트랜잭션 실행

#### 2.2.6 Eventual Consistency + 보상 트랜잭션

분산 환경에서 모든 모듈 간 트랜잭션을 동기로 처리할 수 없다. **최종 일관성(Eventual Consistency)** 을 수용하되, 정합성은 **보상 트랜잭션(Compensating Transaction)** 으로 보장한다.

**Saga 패턴 (Choreography 기반)**:
```
정상 플로우:
  OrderPlacedEvent → [Inventory] reserve()
    → InventoryReservedEvent → [Payment] requestPayment()
      → PaymentCompletedEvent → [Order] confirmOrder()

보상 플로우 (결제 실패):
  PaymentFailedEvent → [Order] cancelOrder()
    → OrderCancelledEvent → [Inventory] restore() (보상)

보상 플로우 (재고 부족):
  InventoryShortageEvent → [Order] failOrder() (보상)
    → [Notification] 재고부족 알림
```

- 각 보상은 독립 트랜잭션으로 실행
- 보상 자체가 실패하면 DLQ + 수동 개입
- 보상 이벤트도 Outbox를 통해 발행 (보상의 신뢰성 보장)

> **Outbox Pattern과 Saga Pattern의 관계**: Outbox는 "이벤트 발행의 신뢰성"을 보장하고, Saga는 "여러 서비스에 걸친 비즈니스 트랜잭션의 정합성"을 보장한다. 둘은 상호 보완적이며, Saga의 각 단계에서 Outbox를 사용하여 이벤트 유실 없는 보상 트랜잭션을 구현한다.

### 2.3 DDD 원칙

#### 2.3.1 Bounded Context 분리

8개 Bounded Context, 각각 독립 모듈:
- **주문(Order)**: 주문 생성, 상태 관리, 주문 이력
- **결제(Payment)**: PG 연동, 결제 처리, 환불
- **상품(Product)**: 상품 등록/수정, 카테고리, 가격 정책
- **재고(Inventory)**: 재고 수량 관리, 예약/차감/복원
- **회원(Member)**: 회원 가입, 인증(JWT), 프로필, 등급
- **파트너(Partner)**: 외부 API 연동, ACL, 파트너 정보 관리
- **알림(Notification)**: 이메일/SMS/카카오 알림톡 발송
- **정산(Settlement)**: 판매자 정산, 수수료 계산, 정산 주기 관리

**도메인 관점에서의 서비스 분리 원칙**: 서비스 분리는 코드 분리가 아니라 "비즈니스 경계"의 분리다. 각 Bounded Context는 자신만의 유비쿼터스 언어, 비즈니스 규칙, 데이터를 보유하며, 다른 Context와의 결합은 도메인 이벤트로만 허용한다.

#### 2.3.2 도메인 패키지 순수성

```
com.pickme.{module}
├── api/            # Controller, DTO (Request/Response)
├── application/    # Application Service, Command/Query Handler
├── domain/         # Entity, VO, Repository Interface, Domain Event, Domain Service
│   ├── model/      # Aggregate Root, Entity, Value Object
│   ├── event/      # Domain Event 정의
│   ├── repository/ # Repository Interface (Port)
│   └── service/    # Domain Service
├── infrastructure/ # Repository 구현체, 외부 연동, Outbox
│   ├── persistence/  # JPA Repository, QueryDSL
│   ├── messaging/    # Kafka Publisher/Consumer
│   ├── external/     # 외부 API 클라이언트 (ACL)
│   └── config/       # 모듈별 설정
└── shared/         # 모듈 내 공유 유틸 (최소화)
```

**ArchUnit 강제**: `domain.*` 패키지는 `api.*`, `infrastructure.*`, `org.springframework.*`를 절대 import하지 않는다. 도메인 로직은 프레임워크와 인프라에 의존하지 않으며, 순수 Java로 테스트 가능하다.

#### 2.3.3 상태 전이 규칙 캡슐화

상태 변경은 반드시 Aggregate Root의 비즈니스 메서드를 통해서만 가능하다. 외부에서 setter로 상태를 직접 변경할 수 없다.

```java
// Order Aggregate Root 예시
public class Order {
    private OrderStatus status;
    
    public void confirm() {
        if (this.status != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalOrderStateException(
                "결제 대기 상태에서만 확정할 수 있습니다. 현재: " + this.status);
        }
        this.status = OrderStatus.PAID;
        this.registerEvent(new OrderConfirmedEvent(this.orderId));
    }
    
    // setter 없음 — 상태 전이는 비즈니스 메서드로만
}
```

> **Tell, Don't Ask**: 외부에서 상태를 조회(Ask)하고 조건 판단 후 상태를 변경하지 않는다. 대신 Aggregate에게 행위를 요청(Tell)하면, Aggregate가 내부에서 유효성을 검증하고 상태를 변경한다.

#### 2.3.4 Anti-Corruption Layer (ACL)

Partner 모듈에서 외부 PG사, 택배사, 카카오 API 등 연동 시 ACL을 적용한다.

- 외부 API 모델 → 내부 도메인 모델 변환 (Translator/Adapter)
- 외부 API 장애, 스키마 변경이 도메인 모델을 오염시키지 않음
- ACL은 infrastructure 패키지에 위치하며, domain은 Port(인터페이스)만 정의

```
외부 PG사 API 응답 → PgPaymentAdapter(ACL) → 내부 PgResponse VO
외부 택배사 API 응답 → DeliveryAdapter(ACL) → 내부 DeliveryInfo VO
카카오 알림톡 API → KakaoNotificationAdapter(ACL) → 내부 NotificationResult VO
```

#### 2.3.5 도메인 이벤트를 통한 느슨한 결합

모듈 간 의존은 도메인 이벤트로만 허용한다. 직접 메서드 호출 대신 이벤트를 발행하면, 발행자는 구독자의 존재를 알 필요가 없다. 새로운 구독자가 추가되어도 발행자 코드는 변경되지 않는다.

---

## 3. Bounded Context 정의

### 3.1 주문 (Order Context)

**Aggregate Root**: `Order`
| 필드 | 타입 | 설명 |
|------|------|------|
| orderId | `OrderId (UUID)` | 주문 식별자 |
| ordererId | `MemberId` | 주문자 (Read Model 스냅샷으로 이름 조회) |
| orderLines | `List<OrderLine>` | 주문 항목 |
| orderStatus | `OrderStatus` | 주문 상태 |
| shippingInfo | `ShippingInfo` | 배송 정보 |
| totalAmount | `Money` | 총 금액 |
| orderedAt | `Instant` | 주문 일시 |

**Value Objects**:
- `OrderId`: UUID 래핑, 식별자
- `OrderLine`: 상품ID, 상품명(스냅샷), 수량, 단가, 소계
- `Money`: 금액 + 통화 (KRW), 연산 메서드 (add, subtract, multiply)
- `ShippingInfo`: 수령인명, 연락처, 주소(Address VO)
- `Address`: 우편번호, 도로명주소, 상세주소
- `OrderStatus`: ENUM — `PLACED → PAYMENT_PENDING → PAID → PREPARING → SHIPPED → DELIVERED → CANCELLED → REFUND_REQUESTED → REFUNDED`

**상태 전이 규칙** (Order 엔티티에 캡슐화):
```
PLACED          → PAYMENT_PENDING  (결제 요청 시)
PAYMENT_PENDING → PAID             (결제 완료 이벤트 수신)
PAYMENT_PENDING → CANCELLED        (결제 실패/타임아웃)
PAID            → PREPARING        (상품 준비 시작)
PREPARING       → SHIPPED          (발송 처리)
SHIPPED         → DELIVERED        (배송 완료)
PAID/PREPARING  → REFUND_REQUESTED (환불 요청)
REFUND_REQUESTED → REFUNDED        (환불 완료 이벤트 수신)
```

**발행 이벤트**:
| 이벤트 | 트리거 | 주요 Payload |
|--------|--------|-------------|
| `OrderPlacedEvent` | 주문 생성 | orderId, ordererId, orderLines[], totalAmount |
| `OrderConfirmedEvent` | 주문 확정 (결제 완료 후) | orderId, confirmedAt |
| `OrderCancelledEvent` | 주문 취소 | orderId, reason, cancelledAt |
| `OrderRefundRequestedEvent` | 환불 요청 | orderId, refundAmount, reason |
| `OrderShippedEvent` | 발송 처리 | orderId, trackingNumber |
| `OrderDeliveredEvent` | 배송 완료 | orderId, deliveredAt |

**구독 이벤트**:
| 이벤트 | 발행자 | 처리 |
|--------|--------|------|
| `PaymentCompletedEvent` | Payment | 주문 상태 → PAID |
| `PaymentFailedEvent` | Payment | 주문 상태 → CANCELLED (보상) |
| `InventoryReservedEvent` | Inventory | 재고 확보 확인 |
| `InventoryShortageEvent` | Inventory | 주문 실패 처리 (보상) |
| `ProductInfoChangedEvent` | Product | product_snapshot Read Model 갱신 |
| `MemberGradeChangedEvent` | Member | member_snapshot Read Model 갱신 |

**CQRS Read Model**:
- `order_schema.product_snapshot` — 상품ID, 상품명, 가격 (상품 이벤트 구독)
- `order_schema.member_snapshot` — 회원ID, 이름, 등급 (회원 이벤트 구독)

---

### 3.2 결제 (Payment Context)

**Aggregate Root**: `Payment`
| 필드 | 타입 | 설명 |
|------|------|------|
| paymentId | `PaymentId (UUID)` | 결제 식별자 |
| orderId | `OrderId` | 연관 주문 (값으로만 보유, 참조 없음) |
| payerId | `MemberId` | 결제자 |
| paymentMethod | `PaymentMethod` | 결제 수단 |
| amount | `Money` | 결제 금액 |
| paymentStatus | `PaymentStatus` | 결제 상태 |
| pgTransactionId | `String` | PG사 거래번호 |
| paidAt | `Instant` | 결제 완료 시각 |

**Value Objects**:
- `PaymentId`: UUID
- `PaymentMethod`: ENUM — `CREDIT_CARD, BANK_TRANSFER, KAKAO_PAY, NAVER_PAY, TOSS_PAY`
- `PaymentStatus`: ENUM — `REQUESTED → PROCESSING → COMPLETED → FAILED → REFUND_REQUESTED → REFUNDED`
- `Money`: 금액 VO (각 모듈에서 동일 구조로 정의)
- `PgResponse`: PG사 응답 래핑

**상태 전이 규칙**:
```
REQUESTED        → PROCESSING       (PG 요청 전송)
PROCESSING       → COMPLETED        (PG 승인 성공)
PROCESSING       → FAILED           (PG 승인 실패)
COMPLETED        → REFUND_REQUESTED (환불 요청 수신)
REFUND_REQUESTED → REFUNDED         (PG 환불 승인)
```

**발행 이벤트**:
| 이벤트 | 트리거 | 주요 Payload |
|--------|--------|-------------|
| `PaymentCompletedEvent` | 결제 성공 | paymentId, orderId, amount, paidAt |
| `PaymentFailedEvent` | 결제 실패 | paymentId, orderId, reason, failedAt |
| `RefundCompletedEvent` | 환불 완료 | paymentId, orderId, refundAmount |

**구독 이벤트**:
| 이벤트 | 발행자 | 처리 |
|--------|--------|------|
| `OrderPlacedEvent` | Order | 결제 요청 생성 (Payment Aggregate 생성) |
| `OrderRefundRequestedEvent` | Order | 환불 처리 시작 |

**ACL**: PG사 API 응답 → 내부 `PgResponse` VO로 변환 (Partner 모듈 경유)

---

### 3.3 상품 (Product Context)

**Aggregate Root**: `Product`
| 필드 | 타입 | 설명 |
|------|------|------|
| productId | `ProductId (UUID)` | 상품 식별자 |
| partnerId | `PartnerId` | 판매 파트너 |
| productName | `ProductName` | 상품명 |
| description | `String` | 상품 설명 |
| price | `ProductPrice` | 가격 정보 |
| category | `Category` | 카테고리 |
| productStatus | `ProductStatus` | 상품 상태 |
| options | `List<ProductOption>` | 상품 옵션 |

**Value Objects**:
- `ProductId`: UUID
- `ProductName`: 1~200자, 빈 문자열 불가 (검증 내장)
- `ProductPrice`: 기본가, 할인가, 할인율 포함. `getSellingPrice()` 메서드
- `Category`: 카테고리 코드 + 이름 (계층 구조)
- `ProductOption`: 옵션명, 옵션값, 추가금액
- `ProductStatus`: ENUM — `DRAFT → ON_SALE → SOLD_OUT → HIDDEN → DISCONTINUED`

**발행 이벤트**:
| 이벤트 | 트리거 | 주요 Payload |
|--------|--------|-------------|
| `ProductRegisteredEvent` | 상품 등록 | productId, productName, price, partnerId |
| `ProductInfoChangedEvent` | 상품 정보 수정 | productId, changedFields |
| `ProductPriceChangedEvent` | 가격 변경 | productId, oldPrice, newPrice |
| `ProductStatusChangedEvent` | 상태 변경 | productId, oldStatus, newStatus |

**구독 이벤트**: 없음 (상품은 독립적, 이벤트 발행만)

---

### 3.4 재고 (Inventory Context)

**Aggregate Root**: `Stock`
| 필드 | 타입 | 설명 |
|------|------|------|
| stockId | `StockId` | 재고 식별자 |
| productId | `ProductId` | 대상 상품 (값) |
| quantity | `Quantity` | 가용 재고 |
| reservedQuantity | `Quantity` | 예약된 재고 |
| totalQuantity | `Quantity` | 전체 재고 |

**Value Objects**:
- `StockId`: UUID
- `Quantity`: 0 이상 정수, 음수 불가 (검증 내장, `subtract()` 시 부족하면 예외)

**재고 연산 규칙** (Stock 엔티티에 캡슐화):
```
reserve(qty)  : quantity -= qty, reservedQuantity += qty  (quantity >= qty 검증)
confirm(qty)  : reservedQuantity -= qty, totalQuantity -= qty  (예약 확정 = 실출고)
cancel(qty)   : reservedQuantity -= qty, quantity += qty  (예약 취소 = 복원, 보상)
restock(qty)  : quantity += qty, totalQuantity += qty  (입고)
```

**분산 락 적용**: `lock:inventory:stock:{productId}` — 동시 재고 차감 시 경합 방지

**발행 이벤트**:
| 이벤트 | 트리거 | 주요 Payload |
|--------|--------|-------------|
| `InventoryReservedEvent` | 재고 예약 성공 | stockId, productId, orderId, reservedQty |
| `InventoryShortageEvent` | 재고 부족 | stockId, productId, orderId, requestedQty, availableQty |
| `InventoryRestoredEvent` | 재고 복원 (보상) | stockId, productId, orderId, restoredQty |
| `StockDepletedEvent` | 재고 소진 (가용 재고 0) | stockId, productId |

**구독 이벤트**:
| 이벤트 | 발행자 | 처리 |
|--------|--------|------|
| `OrderPlacedEvent` | Order | 재고 예약 (reserve) |
| `OrderConfirmedEvent` | Order | 재고 확정 (confirm) |
| `OrderCancelledEvent` | Order | 재고 복원 (cancel) — 보상 트랜잭션 |
| `ProductRegisteredEvent` | Product | 초기 Stock Aggregate 생성 |

---

### 3.5 회원 (Member Context)

**Aggregate Root**: `Member`
| 필드 | 타입 | 설명 |
|------|------|------|
| memberId | `MemberId (UUID)` | 회원 식별자 |
| email | `Email` | 이메일 |
| password | `Password` | BCrypt 해시 |
| name | `MemberName` | 이름 |
| phone | `PhoneNumber` | 연락처 |
| grade | `MemberGrade` | 등급 |
| memberStatus | `MemberStatus` | 회원 상태 |
| registeredAt | `Instant` | 가입 일시 |

**Value Objects**:
- `MemberId`: UUID
- `Email`: 이메일 형식 검증 내장
- `Password`: 해시된 비밀번호, `matches(rawPassword)` 메서드
- `MemberName`: 2~50자
- `PhoneNumber`: 한국 전화번호 형식 검증 (`010-XXXX-XXXX`)
- `MemberGrade`: ENUM — `NORMAL → SILVER → GOLD → VIP → VVIP` (누적 구매액 기준)
- `MemberStatus`: ENUM — `ACTIVE, DORMANT, WITHDRAWN`

**발행 이벤트**:
| 이벤트 | 트리거 | 주요 Payload |
|--------|--------|-------------|
| `MemberRegisteredEvent` | 회원 가입 | memberId, name, email |
| `MemberGradeChangedEvent` | 등급 변경 | memberId, oldGrade, newGrade |
| `MemberWithdrawnEvent` | 회원 탈퇴 | memberId, withdrawnAt |

**구독 이벤트**:
| 이벤트 | 발행자 | 처리 |
|--------|--------|------|
| `OrderConfirmedEvent` | Order | 누적 구매액 업데이트 → 등급 재계산 |

---

### 3.6 파트너 (Partner Context)

**Aggregate Root**: `Partner`
| 필드 | 타입 | 설명 |
|------|------|------|
| partnerId | `PartnerId (UUID)` | 파트너 식별자 |
| businessInfo | `BusinessInfo` | 사업자 정보 |
| contractInfo | `ContractInfo` | 계약 조건 (수수료율 등) |
| partnerStatus | `PartnerStatus` | 파트너 상태 |
| apiCredentials | `ApiCredentials` | 외부 API 인증 정보 |

**Value Objects**:
- `PartnerId`: UUID
- `BusinessInfo`: 사업자등록번호, 상호명, 대표자명
- `ContractInfo`: 수수료율, 정산주기, 계약시작/종료일
- `PartnerStatus`: ENUM — `PENDING → APPROVED → SUSPENDED → TERMINATED`
- `ApiCredentials`: 암호화된 API Key/Secret

**ACL (Anti-Corruption Layer) 대상**:
- PG사 API (토스페이먼츠, KG이니시스 등) → `PgPaymentAdapter`
- 택배사 API (CJ대한통운, 한진 등) → `DeliveryAdapter`
- 카카오 알림톡 API → `KakaoNotificationAdapter`

**발행 이벤트**:
| 이벤트 | 트리거 | 주요 Payload |
|--------|--------|-------------|
| `PartnerApprovedEvent` | 파트너 승인 | partnerId, businessInfo |
| `PartnerSuspendedEvent` | 파트너 정지 | partnerId, reason |

**구독 이벤트**: 없음

---

### 3.7 알림 (Notification Context)

**Aggregate Root**: `Notification`
| 필드 | 타입 | 설명 |
|------|------|------|
| notificationId | `NotificationId (UUID)` | 알림 식별자 |
| recipientId | `MemberId` | 수신자 |
| channel | `NotificationChannel` | 발송 채널 |
| template | `NotificationTemplate` | 템플릿 |
| content | `NotificationContent` | 내용 |
| sendStatus | `SendStatus` | 발송 상태 |
| sentAt | `Instant` | 발송 시각 |

**Value Objects**:
- `NotificationChannel`: ENUM — `EMAIL, SMS, KAKAO_ALIMTALK, APP_PUSH`
- `NotificationTemplate`: 템플릿 코드 + 변수 바인딩
- `SendStatus`: ENUM — `PENDING → SENT → FAILED → RETRY`

**발행 이벤트**:
| 이벤트 | 트리거 | 주요 Payload |
|--------|--------|-------------|
| `NotificationSentEvent` | 알림 발송 성공 | notificationId, channel, recipientId |
| `NotificationFailedEvent` | 알림 발송 실패 | notificationId, channel, reason |

**구독 이벤트** (대부분의 모듈 이벤트를 구독하여 알림 발송):
| 이벤트 | 발행자 | 처리 |
|--------|--------|------|
| `OrderPlacedEvent` | Order | 주문 접수 알림 |
| `PaymentCompletedEvent` | Payment | 결제 완료 알림 |
| `OrderShippedEvent` | Order | 배송 시작 알림 |
| `OrderDeliveredEvent` | Order | 배송 완료 알림 |
| `MemberRegisteredEvent` | Member | 가입 환영 알림 |
| `SettlementCompletedEvent` | Settlement | 정산 완료 알림 (파트너) |
| `InventoryShortageEvent` | Inventory | 재고 부족 알림 (운영) |

---

### 3.8 정산 (Settlement Context)

**Aggregate Root**: `Settlement`
| 필드 | 타입 | 설명 |
|------|------|------|
| settlementId | `SettlementId (UUID)` | 정산 식별자 |
| partnerId | `PartnerId` | 정산 대상 파트너 |
| settlementPeriod | `SettlementPeriod` | 정산 기간 |
| totalSalesAmount | `Money` | 총 매출액 |
| commissionAmount | `Money` | 수수료 |
| settlementAmount | `Money` | 실 정산 금액 |
| settlementStatus | `SettlementStatus` | 정산 상태 |
| settledAt | `Instant` | 정산 완료 시각 |

**Value Objects**:
- `SettlementPeriod`: 시작일, 종료일
- `CommissionRate`: 수수료율 (BigDecimal, 0~100%)
- `SettlementStatus`: ENUM — `CALCULATING → CONFIRMED → TRANSFER_REQUESTED → COMPLETED`

**발행 이벤트**:
| 이벤트 | 트리거 | 주요 Payload |
|--------|--------|-------------|
| `SettlementCalculatedEvent` | 정산 계산 완료 | settlementId, partnerId, amount |
| `SettlementCompletedEvent` | 정산 완료 (이체) | settlementId, partnerId, transferredAt |

**구독 이벤트**:
| 이벤트 | 발행자 | 처리 |
|--------|--------|------|
| `PaymentCompletedEvent` | Payment | 판매 내역 누적 (sales_snapshot) |
| `RefundCompletedEvent` | Payment | 환불 내역 반영 |
| `PartnerApprovedEvent` | Partner | 파트너 정산 정보 스냅샷 생성 |

**CQRS Read Model**:
- `settlement_schema.sales_snapshot` — 일별 매출 집계 (ETL Aggregate Table)
- `settlement_schema.partner_snapshot` — 파트너 계약 정보 (수수료율 등)

---

## 4. 도메인 이벤트 카탈로그

### 4.1 전체 이벤트 흐름도 (핵심 주문 플로우)

```
[고객 주문]
    │
    ▼
OrderPlacedEvent ──────┬──────────────────┬────────────────┐
    │                  │                  │                │
    ▼                  ▼                  ▼                ▼
[Inventory]       [Payment]        [Notification]    [Settlement]
 재고 예약          결제 생성         주문접수 알림       (대기)
    │                  │
    ▼                  │
InventoryReservedEvent │
    │                  │
    ▼                  ▼
    │           PaymentCompletedEvent ──┬──────────┬────────────┐
    │                  │               │          │            │
    │                  ▼               ▼          ▼            ▼
    │            [Order]         [Notification] [Settlement] [Member]
    │            상태→PAID        결제완료 알림   매출 누적    구매액 갱신
    │                  │
    │                  ▼
    │           OrderConfirmedEvent ───┬──────────┐
    │                  │              │          │
    │                  ▼              ▼          ▼
    │           [Inventory]     [Member]    [Notification]
    │            재고 확정       등급 재계산   주문확정 알림
```

### 4.2 이벤트 Payload 계약 (JSON Schema)

#### 공통 이벤트 Envelope
```json
{
  "eventId": "UUID — 전역 고유 식별자 (멱등성 키)",
  "eventType": "com.pickme.order.domain.event.OrderPlacedEvent",
  "aggregateType": "Order",
  "aggregateId": "UUID — Aggregate Root ID",
  "occurredAt": "ISO-8601 Instant",
  "version": 1,
  "payload": { "..." }
}
```

#### OrderPlacedEvent Payload
```json
{
  "orderId": "UUID",
  "ordererId": "UUID",
  "ordererName": "string",
  "orderLines": [
    {
      "productId": "UUID",
      "productName": "string",
      "quantity": 2,
      "unitPrice": 29900,
      "lineTotal": 59800
    }
  ],
  "totalAmount": 59800,
  "currency": "KRW",
  "shippingInfo": {
    "receiverName": "string",
    "phone": "string",
    "address": {
      "zipCode": "string",
      "roadAddress": "string",
      "detail": "string"
    }
  },
  "orderedAt": "2026-04-02T12:00:00Z"
}
```

#### PaymentCompletedEvent Payload
```json
{
  "paymentId": "UUID",
  "orderId": "UUID",
  "payerId": "UUID",
  "amount": 59800,
  "currency": "KRW",
  "paymentMethod": "CREDIT_CARD",
  "pgTransactionId": "string",
  "paidAt": "2026-04-02T12:00:05Z"
}
```

#### InventoryReservedEvent Payload
```json
{
  "stockId": "UUID",
  "productId": "UUID",
  "orderId": "UUID",
  "reservedQuantity": 2,
  "remainingQuantity": 48
}
```

#### InventoryShortageEvent Payload (보상 트리거)
```json
{
  "stockId": "UUID",
  "productId": "UUID",
  "orderId": "UUID",
  "requestedQuantity": 2,
  "availableQuantity": 0
}
```

### 4.3 이벤트 발행/구독 매트릭스

| 이벤트 | Publisher | Consumers |
|--------|-----------|-----------|
| `OrderPlacedEvent` | Order | Inventory, Payment, Notification, Settlement |
| `OrderConfirmedEvent` | Order | Inventory, Member, Notification |
| `OrderCancelledEvent` | Order | Inventory, Payment, Notification |
| `OrderRefundRequestedEvent` | Order | Payment |
| `OrderShippedEvent` | Order | Notification |
| `OrderDeliveredEvent` | Order | Notification |
| `PaymentCompletedEvent` | Payment | Order, Notification, Settlement, Member |
| `PaymentFailedEvent` | Payment | Order, Notification |
| `RefundCompletedEvent` | Payment | Order, Notification, Settlement |
| `ProductRegisteredEvent` | Product | Inventory, Order(snapshot) |
| `ProductInfoChangedEvent` | Product | Order(snapshot) |
| `ProductPriceChangedEvent` | Product | Order(snapshot) |
| `ProductStatusChangedEvent` | Product | Inventory |
| `InventoryReservedEvent` | Inventory | Order |
| `InventoryShortageEvent` | Inventory | Order, Notification |
| `InventoryRestoredEvent` | Inventory | — |
| `StockDepletedEvent` | Inventory | Product, Notification |
| `MemberRegisteredEvent` | Member | Notification, Order(snapshot) |
| `MemberGradeChangedEvent` | Member | Order(snapshot), Notification |
| `MemberWithdrawnEvent` | Member | — |
| `PartnerApprovedEvent` | Partner | Settlement |
| `PartnerSuspendedEvent` | Partner | — |
| `SettlementCalculatedEvent` | Settlement | — |
| `SettlementCompletedEvent` | Settlement | Notification |
| `NotificationSentEvent` | Notification | — (로깅/모니터링) |

---

## 5. 데이터베이스 스키마 전략

### 5.1 Schema-per-Module 레이아웃

```
pickme_db (단일 PostgreSQL 인스턴스, Phase 1~2)
│
├── order_schema
│   ├── orders                  -- 주문 Aggregate
│   ├── order_lines             -- 주문 항목 (Order의 일부)
│   ├── product_snapshot        -- CQRS Read Model (상품 정보)
│   ├── member_snapshot         -- CQRS Read Model (회원 정보)
│   ├── outbox_events           -- Outbox
│   └── processed_events        -- 멱등성
│
├── payment_schema
│   ├── payments                -- 결제 Aggregate
│   ├── payment_history         -- 결제 이력 (상태 변경 로그)
│   ├── outbox_events
│   └── processed_events
│
├── product_schema
│   ├── products                -- 상품 Aggregate
│   ├── product_options         -- 상품 옵션
│   ├── categories              -- 카테고리
│   ├── outbox_events
│   └── processed_events
│
├── inventory_schema
│   ├── stocks                  -- 재고 Aggregate
│   ├── stock_history           -- 재고 변동 이력 (입고/출고/예약)
│   ├── outbox_events
│   └── processed_events
│
├── member_schema
│   ├── members                 -- 회원 Aggregate
│   ├── member_auth             -- 인증 정보 (JWT 토큰 등)
│   ├── purchase_accumulation   -- 누적 구매액 (등급 계산용)
│   ├── outbox_events
│   └── processed_events
│
├── partner_schema
│   ├── partners                -- 파트너 Aggregate
│   ├── partner_contracts       -- 계약 정보
│   ├── api_credentials         -- 외부 API 인증 (암호화)
│   ├── outbox_events
│   └── processed_events
│
├── notification_schema
│   ├── notifications           -- 알림 Aggregate
│   ├── notification_templates  -- 템플릿
│   ├── outbox_events
│   └── processed_events
│
└── settlement_schema
    ├── settlements             -- 정산 Aggregate
    ├── daily_sales_aggregate   -- 일별 매출 집계 (ETL Aggregate Table)
    ├── partner_snapshot        -- 파트너 계약 스냅샷
    ├── commission_rules        -- 수수료 규칙
    ├── outbox_events
    └── processed_events
```

### 5.2 인덱스 전략 — Lock Range 축소가 핵심

인덱스의 가치는 "빠름"이 아니라 **락 범위를 줄이는 것**이다. `SELECT ... FOR UPDATE` 시 인덱스가 없으면 테이블 전체에 락이 걸리지만, 적절한 인덱스가 있으면 해당 행만 락이 걸린다.

```sql
-- 재고: productId 기반 동시성 제어 (FOR UPDATE 시 lock range 최소화)
CREATE UNIQUE INDEX idx_stocks_product_id ON inventory_schema.stocks (product_id);

-- 주문: 주문자 + 상태 복합 인덱스 (조회 최적화)
CREATE INDEX idx_orders_orderer_status ON order_schema.orders (orderer_id, order_status);

-- 주문: 날짜 범위 조회 (파티셔닝 대비)
CREATE INDEX idx_orders_ordered_at ON order_schema.orders (ordered_at);

-- 결제: 주문ID 역조회
CREATE INDEX idx_payments_order_id ON payment_schema.payments (order_id);

-- Outbox: 미발행 이벤트 빠른 조회 (Partial Index)
CREATE INDEX idx_outbox_unpublished ON {module}_schema.outbox_events (published, created_at)
    WHERE published = FALSE;

-- Processed Events: PK = event_id → 멱등성 체크에 추가 인덱스 불필요
```

### 5.3 Connection Pool 전략

> **DB 병목의 시작은 항상 커넥션 풀이다.** 커넥션 풀이 포화되면 대기 스레드가 급증하고, 응답 시간은 선형이 아니라 **기하급수적으로** 증가한다. 병목은 항상 "기다림"의 형태로 나타난다.

**Phase 1 (Modular Monolith, 단일 DB)**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 40          # 8모듈 × 5 동시 커넥션
      minimum-idle: 10
      connection-timeout: 3000       # 3초 (빠른 실패, 무한 대기 방지)
      idle-timeout: 600000           # 10분
      max-lifetime: 1800000          # 30분
      leak-detection-threshold: 5000 # 5초 이상 미반환 시 경고
```

**풀 크기 산정 공식**: `동시 커넥션 = TPS × 평균 쿼리 시간`  
- 예: TPS 100, 평균 쿼리 50ms → 필요 커넥션 = 100 × 0.05 = 5개
- 피크 TPS 1,000 → 필요 커넥션 = 1,000 × 0.05 = 50개 → Pool Max 확보 필요
- 단, `cores × 2 + effective_spindle_count` 공식도 참고

**반드시 함께 봐야 하는 지표 묶음**:
- Active Connections / Total Connections (풀 사용률)
- Connection Wait Time (커넥션 획득 대기 시간)
- Query Execution Time (쿼리 실행 시간)
- Thread Count / Thread Wait (스레드 대기 현황)

**Phase 3 (MSA, DB 분리)**: 서비스별 독립 DB, 독립 커넥션 풀. 서비스 트래픽에 맞춘 풀 크기 개별 튜닝.

### 5.4 DB Replication 전략

#### Phase 1: 단일 리더 복제 (Single Leader)

```
Primary (Read/Write) ──동기/비동기 복제──→ Replica 1 (Read Only)
                      ──동기/비동기 복제──→ Replica 2 (Read Only)
```

- **Write**: Primary로 라우팅
- **Read**: Replica로 라우팅 (`@Transactional(readOnly = true)`)
- Replication Lag 감안 (CQRS Read Model은 Eventual Consistency이므로 문제 없음)
- 복제 지연 모니터링 필수 (pg_stat_replication)

#### Phase 3: 모듈별 독립 DB

- 모듈별 독립 DB 인스턴스, 각각 Single Leader Replication
- 정산/통계 등 Heavy Read 모듈은 Replica 추가 증설

#### 멀티 리더 / 리더 없는 복제

- **멀티 리더**: 지리적 분산이 필요한 경우에만 고려 (한국 단일 리전이면 불필요). 충돌 해소(conflict resolution) 복잡성이 높으므로 신중하게 판단
- **리더 없는 복제**: Quorum 기반 (W + R > N). 극단적 가용성이 필요한 경우에만 고려. 현 단계에서는 불필요

### 5.5 CDC (Change Data Capture) 전략

> CDC는 DB 변경을 실시간으로 감지하여 다른 시스템에 전파하는 기술이다. Polling 방식 대비 지연 시간이 짧고, 원천 DB에 부하를 주지 않는다.

**Phase 2~3**: Debezium + Kafka Connect

```
[Module DB] ──CDC──→ [Debezium] ──→ [Kafka Topic] ──→ [Consumer]
```

**CDC + Outbox 조합**: Outbox 테이블의 INSERT를 CDC로 감지 → Kafka 발행. Polling Publisher를 대체하여 이벤트 전달 지연을 수십ms로 단축.

**CDC + CQRS 조합**: 원천 테이블 변경 감지 → Read Model 자동 동기화. 이벤트 핸들러를 별도로 구현하지 않아도 됨.

**CDC + Event Sourcing 조합**: Event Store의 append-only 로그를 CDC로 스트리밍 → Projection 자동 갱신.

### 5.6 ETL + Aggregate Table (통계/집계)

> **통계와 집계는 왜 느릴까?** 원천 테이블에서 실시간 집계하면 Full Scan + Group By가 발생하며, 트래픽이 증가할수록 집계 쿼리가 일반 쿼리의 성능을 잠식한다. 이것은 튜닝으로 해결 안 되는 병목이다.

정산 모듈의 `daily_sales_aggregate`는 **ETL Aggregate Table**:

```sql
-- ETL 과정: Extract(이벤트 수신) → Transform(집계 로직) → Load(Aggregate Table 적재)
CREATE TABLE settlement_schema.daily_sales_aggregate (
    aggregate_date  DATE NOT NULL,
    partner_id      UUID NOT NULL,
    total_orders    INT NOT NULL DEFAULT 0,
    total_sales     BIGINT NOT NULL DEFAULT 0,   -- 원 단위
    total_refunds   BIGINT NOT NULL DEFAULT 0,
    net_sales       BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (aggregate_date, partner_id)
);
```

- **실시간 누적**: PaymentCompletedEvent, RefundCompletedEvent 수신 시 해당 일자의 Aggregate Row UPSERT
- **배치 검증**: 일 1회 Reconciliation 배치로 원천 데이터와 Aggregate 정합성 검증
- **대시보드/리포트**: Aggregate Table에서 조회 (원천 테이블 부하 방지)

---

## 6. 트래픽/성능 전략

### 6.1 트래픽 이해

> **트래픽의 정의**: 단위 시간당 시스템에 유입되는 요청의 양. 단순히 DAU로 측정하면 안 되며, TPS(초당 처리량)와 피크 배율을 함께 고려해야 한다.

**DAU가 늘지 않았는데 왜 서버가 터졌을까?** DAU는 동일하더라도 특정 시간대에 요청이 집중되면 순간 TPS가 폭증한다. 타임딜, 이벤트 시작 시각에 DAU의 30%가 동시에 접속하면 평소 대비 50배 스파이크가 발생할 수 있다.

**기능 하나 추가했을 뿐인데 트래픽은 왜 3배가 됐을까?** 상품 상세 페이지에 "실시간 재고 수량"을 추가하면, 기존 1회 조회(상품)에서 2회 조회(상품 + 재고)로 증가한다. 사용자당 요청이 2배가 되고, DB 부하는 3배(상품 캐시 히트 + 재고 캐시 미스 + DB 쿼리)가 될 수 있다.

**평균 TPS는 의미 없다.** 시스템은 평균이 아니라 피크에서 죽는다. 99th percentile TPS가 설계 기준이어야 한다.

**트래픽 증가 → 시스템 내부 비용 폭증**: 트래픽이 2배 증가하면 시스템 내부 비용(DB 커넥션 경합, 캐시 미스 비율, 락 대기 시간, GC 빈도)은 2배가 아니라 4~8배로 증가한다. 이것이 기하급수적 병목 증가의 원인이다.

### 6.2 트래픽 프로파일

| 지표 | 평상시 | 피크 (타임딜/이벤트) | 비고 |
|------|--------|---------------------|------|
| TPS (주문) | 50~100 | 3,000~5,000 | 50배 스파이크 |
| TPS (상품 조회) | 500~1,000 | 10,000~30,000 | Read Heavy |
| TPS (재고 차감) | 50~100 | 3,000~5,000 | Write Heavy + Lock |
| DB Connection | 20~30 | 40 (Pool Max) | 포화 시 지수적 성능 저하 |

### 6.3 병목 유형별 대응

#### 6.3.1 Read 병목 (상품 조회)

> **읽기 병목의 본질**: DB에서 동일 데이터를 반복 조회하면서 커넥션 풀과 I/O를 소모하는 것. 데이터가 변경되지 않았다면 DB까지 갈 이유가 없다.

- **Redis Cache (Cache Aside 패턴)**: 조회 시 캐시 확인 → Miss 시 DB 조회 + 캐시 저장
- **Cache Stampede 방지**: Redis 분산 락으로 동시에 하나의 스레드만 DB 조회 (나머지는 락 대기 후 캐시 히트)
- **Local Cache (Caffeine)**: 초고빈도 조회 데이터 (카테고리 트리 등)는 JVM 내 캐시
- **Read Replica 라우팅**: `@Transactional(readOnly = true)` → Replica DB로 자동 라우팅

#### 6.3.2 Write 병목 (주문/결제)

> **쓰기 병목의 본질**: 모든 쓰기가 Primary DB의 단일 경로를 통과하며, 트랜잭션 커밋이 직렬화되는 것.

- **비동기 처리**: 주문 접수 → 즉시 응답 (주문 ID 반환) → 이후 이벤트 기반 비동기 처리 (재고 예약 → 결제)
- **Batch Insert**: Outbox 이벤트 벌크 인서트로 DB I/O 횟수 최소화
- **DB 파티셔닝**: `orders` 테이블 월별 Range 파티셔닝 (Phase 2+, 오래된 데이터와 최신 데이터의 I/O 분리)

#### 6.3.3 Lock Contention (재고)

> **락 경합의 본질**: 동시에 같은 자원을 변경하려는 요청이 직렬화되면서 대기 시간이 폭증하는 것. 인기 상품 타임딜 시 수천 요청이 동시에 같은 재고 row를 차감하려 한다.

- **Redis 분산 락**: Redisson `SETNX` 기반, productId 단위 세밀한 락 (테이블 수준 락 방지)
- **Optimistic Locking + Retry**: JPA `@Version` 필드, 최대 3회 재시도 (락 경합이 낮을 때 효율적)
- **인덱스로 Lock Range 축소**: `SELECT ... WHERE product_id = ? FOR UPDATE`에서 인덱스가 없으면 테이블 풀 락 → `idx_stocks_product_id` 인덱스 필수
- **재고 Pre-deduction (Redis Lua Script)**: Redis에 재고 수량 캐시 → Redis에서 원자적 차감 (Lua Script) → DB 비동기 반영. DB 락 자체를 회피.

```lua
-- Redis Lua Script: 원자적 재고 차감
local stock = tonumber(redis.call('GET', KEYS[1]))
local qty = tonumber(ARGV[1])
if stock >= qty then
    redis.call('DECRBY', KEYS[1], qty)
    return 1  -- 성공
else
    return 0  -- 재고 부족
end
```

#### 6.3.4 Cache 부재 (Cache Miss Storm)

> **캐싱의 무서운 이면**: 캐시가 갑자기 사라지면(서버 재시작, TTL 동시 만료), 모든 요청이 DB로 몰려 순간 부하가 폭발한다. 캐시는 있을 때는 성능을 높이지만, 없을 때는 캐시 없는 것보다 더 나쁜 상황을 만든다.

- **Warm-up**: 서버 시작 시 인기 상품 Top N 캐시 사전 로딩
- **TTL Jitter**: 캐시 만료 시간에 랜덤 오프셋 추가 (동시 만료 → 동시 DB 쿼리 방지)
  ```
  TTL = baseTimeout + random(0, baseTimeout * 0.1)
  ```
- **Null Object Cache**: DB에 없는 데이터도 짧은 TTL(1분)로 캐시 (캐시 관통 공격 방지)
- **Redis Cluster HA**: Master 장애 시 Sentinel/Cluster가 자동 Failover

#### 6.3.5 Message Processing Delay (이벤트 지연)

> **메시지 처리 지연의 본질**: 이벤트 발행 속도 > 소비 속도일 때 큐에 메시지가 적체되어 처리 지연이 누적되는 것.

- **Kafka Batch Consumer**: `max.poll.records`로 배치 수신, `fetch.min.bytes`로 네트워크 왕복 최소화
- **Consumer 스케일링**: 파티션 수에 비례하여 Consumer 인스턴스 추가 (Consumer Group 리밸런싱)
- **토픽 분리**: 결제/재고 이벤트는 별도 토픽 + 더 많은 파티션, 알림은 일반 토픽
- **Backpressure**: `max.poll.interval.ms` 조정으로 처리 속도에 맞춘 폴링 제어
- **Consumer Lag 모니터링**: Kafka Consumer Lag 기반 Auto Scaling (Prometheus + KEDA)

### 6.4 Redis 캐싱 전략 상세

> **왜 Redis를 빠르다고 할까?** 모든 데이터를 메모리에 저장하고, 싱글 스레드 이벤트 루프로 네트워크 I/O를 처리하기 때문. DB의 디스크 I/O + 파싱 + 최적화 과정을 건너뛴다. **캐싱의 기준점**: "이 데이터를 다시 계산/조회하는 비용 > 캐시에 저장/조회하는 비용"일 때 캐싱한다.

| 캐시 대상 | Key 패턴 | TTL | 전략 | 갱신 |
|-----------|---------|-----|------|------|
| 상품 상세 | `product:{id}` | 5분 + Jitter | Cache Aside | 이벤트 기반 무효화 |
| 상품 목록 (카테고리) | `products:category:{id}:page:{n}` | 3분 | Cache Aside | TTL 만료 |
| 재고 수량 | `stock:{productId}` | 없음 (영구) | Write Through | 재고 변경 시 동기 갱신 |
| 회원 세션 | `session:{sessionId}` | 30분 | — | 접근 시 TTL 연장 |
| Rate Limit | `ratelimit:{ip/userId}` | 1분 | Sliding Window | 자동 만료 |
| 분산 락 | `lock:{module}:{agg}:{id}` | 5초 | Redisson | 자동 해제 |

---

## 7. 가용성 패턴

### 7.1 가용성 기본 컨셉

> 트래픽이 높아지면 시스템 내부 자원(CPU, 메모리, 커넥션, 스레드)이 포화되고, 응답 시간이 급격히 증가하며, 최종적으로 서비스 불가 상태가 된다. 가용성(Availability) 패턴은 이 과정을 지연시키거나 방지하는 안전장치다.

### 7.2 Circuit Breaker (Resilience4j)

> **의존하던 서비스가 갑자기 느려진 경우**: PG사 API가 5초 이상 응답하지 않으면, 호출하는 쪽의 스레드가 5초씩 블로킹되어 스레드 풀이 포화된다. 이 상태가 지속되면 결제 모듈뿐 아니라 전체 시스템이 마비된다. Circuit Breaker는 실패율이 임계치를 넘으면 호출 자체를 차단(Open)하여 장애 전파를 방지한다.

**적용 대상**: 외부 API 호출 (PG사, 택배사, 카카오 알림톡)

```yaml
resilience4j:
  circuitbreaker:
    instances:
      pg-payment:
        slidingWindowSize: 10
        failureRateThreshold: 50          # 50% 실패 시 Open
        waitDurationInOpenState: 30s      # 30초 후 Half-Open
        permittedNumberOfCallsInHalfOpenState: 3
        slowCallRateThreshold: 80         # 80% 느린 호출 시 Open
        slowCallDurationThreshold: 5s     # 5초 이상이면 느린 호출
      delivery-api:
        slidingWindowSize: 20
        failureRateThreshold: 60
        waitDurationInOpenState: 60s
      kakao-notification:
        slidingWindowSize: 10
        failureRateThreshold: 70
        waitDurationInOpenState: 15s
```

**Fallback 전략**:
- PG 결제 실패 → 다른 PG사로 자동 전환 (Multi-PG)
- 택배사 API 실패 → 큐에 적재 후 재시도
- 알림 실패 → DLQ + 수동 재발송

### 7.3 Rate Limiter

> **시끄러운 이웃 문제(Noisy Neighbor)**: 한 사용자 또는 한 IP의 과도한 요청이 다른 사용자의 서비스 품질을 저하시키는 현상. Rate Limiter는 요청 빈도를 제한하여 공정한 자원 분배를 보장한다.

**구현**: Redis 기반 Sliding Window Counter

| 대상 | 기준 | 제한 | 응답 |
|------|------|------|------|
| API 전체 | IP당 | 100 req/min | 429 Too Many Requests |
| 주문 생성 | 회원당 | 10 req/min | 429 + "잠시 후 다시 시도해주세요" |
| 결제 요청 | 회원당 | 5 req/min | 429 |
| 상품 조회 | IP당 | 300 req/min | 429 |
| 로그인 시도 | IP+이메일 | 5 req/5min | 423 Locked (일시 차단) |

### 7.4 Timeout 설정

> **Timeout의 핵심**: 무한 대기를 방지하고, 문제가 있는 호출을 빠르게 실패시켜 자원을 회수한다. Timeout 없이 외부 API를 호출하면, 해당 API 장애 시 호출 스레드가 무한 블로킹되어 시스템 전체가 마비될 수 있다.

| 구간 | Timeout | 비고 |
|------|---------|------|
| API Gateway → Backend | 10s | 전체 요청 타임아웃 |
| Backend → PostgreSQL | 3s | HikariCP connectionTimeout |
| Backend → Redis | 1s | Lettuce commandTimeout |
| Backend → PG사 API | 5s | Circuit Breaker slowCallDurationThreshold와 연동 |
| Backend → Kafka | 3s | Kafka Producer `delivery.timeout.ms` |
| Kafka Consumer Poll | 30s | `max.poll.interval.ms` 기본값, 초과 시 리밸런싱 |
| 분산 락 TTL | 5s | Deadlock 방지 |
| 분산 락 waitTime | 3s | 락 획득 대기 최대 시간 |

### 7.5 Noisy Neighbor 방지

- **Schema 격리**: 모듈별 스키마 → 한 모듈의 Heavy Query가 다른 모듈에 미치는 영향 최소화
- **Connection Pool 분리**: Phase 2에서 모듈별 독립 DataSource (커넥션 풀 격리)
- **Thread Pool 격리**: `@Async` 처리 시 모듈별 전용 ThreadPoolTaskExecutor
- **Kafka Topic 분리**: 모듈별 독립 토픽 (한 모듈의 이벤트 폭발이 다른 모듈 Consumer Lag에 영향 안 줌)
- **Bulkhead 패턴 (Phase 3)**: Resilience4j Bulkhead로 동시 호출 수 제한

### 7.6 Saga 패턴 + 보상 트랜잭션 상세

> **분산 환경에서 트랜잭션을 활용할 수 없는 이유**: 서로 다른 DB(또는 스키마)에 걸친 2PC(Two-Phase Commit)는 성능 저하와 가용성 감소를 유발한다. Saga 패턴은 각 서비스의 로컬 트랜잭션을 순차 실행하고, 실패 시 이전 단계를 보상 트랜잭션으로 롤백한다.

**주문 Saga (Choreography)**:
```
정상 플로우:
1. OrderPlacedEvent        → [Inventory] reserve()
2. InventoryReservedEvent  → [Payment] requestPayment()
3. PaymentCompletedEvent   → [Order] confirmOrder()
4. OrderConfirmedEvent     → [Inventory] confirm(), [Member] addPurchase()

보상 플로우 (결제 실패):
3'. PaymentFailedEvent     → [Order] cancelOrder()
4'. OrderCancelledEvent    → [Inventory] restore() (보상)

보상 플로우 (재고 부족):
2'. InventoryShortageEvent → [Order] failOrder() (보상)
                           → [Notification] 재고부족 알림
```

**보상 트랜잭션 원칙**:
- 각 보상은 독립 로컬 트랜잭션으로 실행 (다른 서비스 트랜잭션과 무관)
- 보상 자체가 실패하면 DLQ + 수동 개입 (운영팀 알림)
- 보상 이벤트도 Outbox를 통해 발행 (보상의 신뢰성 보장)
- 일 1회 정합성 검증 배치로 좀비 주문/결제 탐지 및 보정

### 7.7 동기 vs 비동기 통신 판단 기준

| 구분 | 동기 (Sync) | 비동기 (Async) |
|------|-------------|----------------|
| **정의** | 호출자가 응답을 받을 때까지 블로킹 | 호출자가 요청만 보내고 즉시 반환 |
| **사용 시점** | 즉시 응답 필요, 강한 일관성 | 최종 일관성 허용, 느슨한 결합 |
| **pick-me 적용** | 모듈 내부 호출만 동기 | 모듈 간 통신은 전부 비동기 (이벤트) |
| **장점** | 단순, 디버깅 용이, 일관성 보장 | 결합도 제거, 확장성, 장애 격리 |
| **단점** | 결합, 장애 전파, 확장 어려움 | 복잡성, 디버깅 어려움, 지연 |

**pick-me 원칙**: 모듈 간 통신은 100% 비동기 (도메인 이벤트). 동기 호출은 모듈 내부(Controller → Service → Repository)에서만 허용.

### 7.8 Kafka를 활용한 서비스 간 결합도 완화

**Kafka를 Phase 1부터 채택하는 이유**:
- 이벤트 리플레이 가능 (Consumer가 offset을 되돌려 과거 이벤트 재처리 → 장애 복구, Read Model 재구축에 필수)
- 높은 처리량 (초당 수십만 메시지, 타임딜 스파이크 대응)
- Consumer Group 기반 자동 파티션 리밸런싱 (Consumer 추가/제거 시 자동 분배)
- 토픽 기반 구독으로 Publisher-Consumer 완전 분리
- 파티션 키(orderId, productId) 기반 순서 보장 (같은 주문의 이벤트는 항상 같은 파티션에서 순서대로 처리)
- AWS MSK(Managed Kafka) 사용으로 운영 부담 최소화 (별도 Kafka 클러스터 운영 불필요)
- Phase 1부터 Kafka를 사용하면 Phase 3 MSA 전환 시 메시지 브로커 마이그레이션 비용이 제로

**서비스 간 결합도를 낮추는 설계**:
- 이벤트는 "발생한 사실(Fact)"을 기술, 명령(Command)이 아님
- Publisher는 Consumer의 존재를 모름 → 새 Consumer 추가 시 Publisher 변경 없음
- 이벤트 스키마 버저닝으로 하위 호환성 유지
- 토픽 네이밍 컨벤션: `pickme.{module}.{event-type}` (예: `pickme.order.order-placed`)

**핵심 Kafka 설정**:
```yaml
spring:
  kafka:
    producer:
      acks: all                          # 모든 ISR 복제 완료 후 ACK
      enable-idempotence: true           # 중복 발행 방지
      retries: 3
    consumer:
      auto-offset-reset: earliest        # 신규 Consumer는 처음부터 읽기
      enable-auto-commit: false          # 수동 offset 커밋 (처리 완료 후)
      max-poll-records: 50               # 배치 수신
    listener:
      ack-mode: manual                   # 수동 ACK
```

---

## 8. 팀 컨벤션 및 개발 표준

생산성과 코드 품질을 유지하기 위해 아래 표준화된 규칙을 프로젝트 전체에 적용한다.

### 8.1 브랜치 전략 (GitHub Flow 기반)

```
main ─────────────────────────────────────────────── (항상 배포 가능 상태)
  └── feat/PM-1-gradle-multi-module ──── PR ──── merge
  └── feat/PM-2-docker-compose-infra ─── PR ──── merge
  └── fix/PM-15-npe-on-payment ───────── PR ──── merge
  └── hotfix/PM-99-db-connection-leak ── PR ──── merge
```

**브랜치 명명 규칙**: `type/PM-{issue-number}-short-description`

| Type | 용도 | 예시 |
|------|------|------|
| `feat/` | 새로운 기능 개발 | `feat/PM-3-product-aggregate` |
| `fix/` | 버그 수정 | `fix/PM-15-npe-on-payment` |
| `refactor/` | 코드 리팩토링 (기능 변경 없음) | `refactor/PM-20-order-service-cleanup` |
| `hotfix/` | 프로덕션 긴급 버그 수정 | `hotfix/PM-99-db-connection-leak` |
| `chore/` | 빌드, 의존성, CI 설정 등 | `chore/PM-5-github-actions-ci` |
| `test/` | 테스트 코드 추가/수정 | `test/PM-12-order-saga-test` |
| `docs/` | 문서 수정 | `docs/PM-1-prd-update` |

**규칙**:
- `main` 브랜치에 직접 push 금지 (Branch Protection Rule)
- 모든 변경은 PR을 통해서만 `main`에 merge
- 브랜치명은 소문자, 단어 구분은 하이픈(`-`)
- Issue 번호는 `PM-{번호}` 형식 (pick-me 프로젝트 prefix)

### 8.2 커밋 메시지 규칙 (Conventional Commits)

**형식**: `type(scope): subject (#issue-number)`

```
feat(order): 주문 생성 API 및 OrderPlacedEvent 발행 구현 (#PM-8)

- Order Aggregate Root에 상태 전이 규칙 캡슐화
- Transactional Outbox 패턴으로 이벤트 발행
- OrderId, Money, ShippingInfo Value Object 정의
```

**Type 목록**:

| Type | 설명 | 예시 |
|------|------|------|
| `feat` | 새로운 기능 | `feat(product): 상품 등록 API 구현` |
| `fix` | 버그 수정 | `fix(inventory): 재고 차감 동시성 이슈 해결` |
| `refactor` | 리팩토링 (기능 변경 없음) | `refactor(payment): PG 어댑터 인터페이스 분리` |
| `test` | 테스트 추가/수정 | `test(order): Saga 보상 트랜잭션 시나리오 테스트` |
| `chore` | 빌드, 설정, 의존성 | `chore(gradle): Spring Boot 3.5.0 버전 업그레이드` |
| `docs` | 문서 | `docs(prd): Docker Compose 구성 섹션 추가` |
| `style` | 코드 포맷팅 (기능 변경 없음) | `style(common): import 순서 정리` |
| `perf` | 성능 개선 | `perf(inventory): Redis Lua Script 재고 Pre-deduction` |
| `ci` | CI/CD 설정 | `ci: GitHub Actions 빌드 파이프라인 추가` |

**Scope**: 변경된 모듈명 사용 (`order`, `payment`, `product`, `inventory`, `member`, `partner`, `notification`, `settlement`, `common`, `infra`, `gradle`)

**규칙**:
- 제목은 50자 이내, 끝에 마침표(`.`) 금지
- 제목은 명령형으로 작성 ("추가", "수정", "제거" — "추가함", "수정했음" 아님)
- 본문(선택)은 72자마다 줄바꿈, **"무엇을"** 과 **"왜"** 변경했는지 설명
- 본문과 제목 사이 빈 줄 1개
- Issue 번호는 제목 끝에 `(#PM-번호)` 형식으로 참조

### 8.3 PR (Pull Request) 규칙

**PR 제목**: `[Type] 본문 요약 (#PM-이슈번호)`
```
[Feat] 주문 생성 API 및 Saga 이벤트 발행 (#PM-8)
[Fix] 재고 차감 시 분산 락 미적용 이슈 해결 (#PM-15)
[Refactor] Payment 모듈 ACL 패턴 적용 (#PM-20)
```

**PR 본문 템플릿** (`.github/PULL_REQUEST_TEMPLATE.md`에 설정):

```markdown
## Motivation (작업 배경)
<!-- 왜 이 PR이 필요한가? 어떤 문제를 해결하는가? -->

## Modifications (변경 사항)
<!-- 핵심적으로 변경된 로직을 설명 -->
-
-

## Testing (테스트 방법)
<!-- 검증 방법 및 결과 (스크린샷, 테스트 로그 등) -->
- [ ] 단위 테스트 통과
- [ ] 통합 테스트 통과
- [ ] `docker compose up` 정상 동작 확인

## Note (참고 사항)
<!-- 리뷰어가 알아야 할 추가 맥락 (선택) -->
```

**PR 규칙**:
- PR 당 변경 파일 400줄 이하 권장 (초과 시 분리 검토)
- `main` 브랜치로의 PR은 최소 1명 리뷰 승인 필수
- CI 빌드(컴파일 + 테스트 + ArchUnit) 통과 필수
- Merge 방식: **Squash and Merge** (커밋 이력 깔끔하게 유지)
- Merge 후 원격 브랜치 자동 삭제

### 8.4 코딩 컨벤션 (Java / Spring Boot)

#### 8.4.1 네이밍 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| 클래스 | PascalCase | `OrderService`, `PaymentCompletedEvent` |
| 인터페이스 | PascalCase (접두사 `I` 금지) | `OrderRepository`, `PaymentGateway` |
| 메서드 | camelCase, 동사로 시작 | `placeOrder()`, `reserveStock()` |
| 변수 | camelCase | `orderId`, `totalAmount` |
| 상수 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE` |
| 패키지 | 소문자, 단수형 | `com.pickme.order.domain.model` |
| DB 테이블 | snake_case, 복수형 | `orders`, `order_lines`, `outbox_events` |
| DB 컬럼 | snake_case | `order_id`, `created_at`, `order_status` |
| Kafka 토픽 | 점(.) 구분, 소문자 | `pickme.order.events` |
| Redis Key | 콜론(:) 구분 | `product:{id}`, `lock:inventory:stock:{productId}` |

#### 8.4.2 RESTful API 규칙

**URI 설계**:
- 명사 복수형 사용, 소문자와 하이픈(`-`) 조합
- 동사 금지 (행위는 HTTP Method로 표현)
- 리소스 계층은 최대 2 depth

```
GET    /api/v1/orders                          # 주문 목록 조회
POST   /api/v1/orders                          # 주문 생성
GET    /api/v1/orders/{orderId}                # 주문 상세 조회
PATCH  /api/v1/orders/{orderId}/cancel         # 주문 취소 (상태 변경)
POST   /api/v1/orders/{orderId}/refund         # 환불 요청

GET    /api/v1/products                        # 상품 목록
GET    /api/v1/products/{productId}            # 상품 상세
POST   /api/v1/products                        # 상품 등록

GET    /api/v1/members/{memberId}              # 회원 정보
POST   /api/v1/auth/login                      # 로그인
POST   /api/v1/auth/signup                     # 회원가입
```

**응답 형식**:
```json
// 성공 응답
{
  "success": true,
  "data": { ... },
  "message": null
}

// 에러 응답
{
  "success": false,
  "data": null,
  "error": {
    "code": "ORDER_NOT_FOUND",
    "message": "주문을 찾을 수 없습니다.",
    "timestamp": "2026-04-02T12:00:00Z"
  }
}
```

**HTTP Status Code 사용 기준**:

| Status | 용도 |
|--------|------|
| `200 OK` | 조회, 수정 성공 |
| `201 Created` | 생성 성공 (Location 헤더 포함) |
| `204 No Content` | 삭제 성공 |
| `400 Bad Request` | 요청 파라미터/바디 유효성 실패 |
| `401 Unauthorized` | 인증 실패 (토큰 없음/만료) |
| `403 Forbidden` | 권한 없음 |
| `404 Not Found` | 리소스 없음 |
| `409 Conflict` | 상태 충돌 (이미 취소된 주문 재취소 등) |
| `429 Too Many Requests` | Rate Limit 초과 |
| `500 Internal Server Error` | 서버 내부 오류 |

#### 8.4.3 코드 작성 규칙

**Early Return**: 중첩 if문 대신 예외를 먼저 처리하고 반환
```java
// Bad
public void process(Order order) {
    if (order != null) {
        if (order.isPaid()) {
            // 로직...
        }
    }
}

// Good
public void process(Order order) {
    if (order == null) throw new OrderNotFoundException();
    if (!order.isPaid()) throw new InvalidOrderStateException("결제 완료 상태가 아닙니다");
    // 로직...
}
```

**Magic Number 금지**: 의미를 알 수 없는 숫자/문자열은 상수로 추출
```java
// Bad
if (retryCount > 3) { ... }

// Good
private static final int MAX_RETRY_COUNT = 3;
if (retryCount > MAX_RETRY_COUNT) { ... }
```

**Custom Exception**: 범용 `RuntimeException`을 던지지 않고, 도메인 맞춤 예외 사용
```java
// Bad
throw new RuntimeException("주문을 찾을 수 없습니다");

// Good — 모듈별 예외 계층
public class OrderNotFoundException extends BusinessException {
    public OrderNotFoundException(OrderId orderId) {
        super(ErrorCode.ORDER_NOT_FOUND, "주문을 찾을 수 없습니다: " + orderId);
    }
}
```

**예외 계층 구조**:
```
BusinessException (추상, @ResponseStatus 없음)
├── OrderNotFoundException (404)
├── InvalidOrderStateException (409)
├── InsufficientStockException (409)
├── PaymentFailedException (502)
├── DuplicateEventException (무시, 200 반환)
└── ExternalApiException (502/503)
```

**기타 코드 규칙**:
- `@Getter` 사용, `@Setter` / `@Data` 금지 (상태 변경은 비즈니스 메서드로만)
- Entity에 `public` 기본 생성자 대신 `protected` 기본 생성자 (JPA 요구사항)
- `Optional`은 반환 타입으로만 사용 (파라미터, 필드에 사용 금지)
- 컬렉션 반환 시 `null` 대신 빈 컬렉션 (`Collections.emptyList()`)
- `==` 대신 `Objects.equals()` 사용 (VO 비교)
- 한 메서드는 한 가지 일만 수행 (20줄 이하 권장)
- `import *` 금지

### 8.5 테스트 컨벤션

**테스트 메서드 네이밍**: `메서드명_시나리오_기대결과` (한글 허용)
```java
@Test
void 주문생성_정상요청_OrderPlacedEvent발행() { ... }

@Test
void 주문생성_재고부족_InsufficientStockException() { ... }

@Test
void 재고차감_동시요청_분산락으로_정합성보장() { ... }
```

**테스트 구조**: Given-When-Then (BDD 스타일)
```java
@Test
void 주문생성_정상요청_OrderPlacedEvent발행() {
    // Given
    PlaceOrderCommand cmd = PlaceOrderCommand.of(...);
    
    // When
    Order order = Order.place(cmd);
    
    // Then
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
    assertThat(order.getDomainEvents()).hasSize(1);
    assertThat(order.getDomainEvents().get(0)).isInstanceOf(OrderPlacedEvent.class);
}
```

**테스트 종류별 위치**:

| 종류 | 위치 | 어노테이션 | 대상 |
|------|------|-----------|------|
| 단위 테스트 | `src/test/java` | `@Test` (순수 JUnit) | Domain Model, VO, Domain Service |
| 통합 테스트 | `src/test/java` | `@SpringBootTest` | Service + Repository + Kafka |
| ArchUnit 테스트 | `pickme-archunit/src/test` | `@AnalyzeClasses` | 모듈 경계, 도메인 순수성 |
| API 테스트 | `src/test/java` | `@WebMvcTest` | Controller + DTO 직렬화 |

### 8.6 Git 워크플로 요약

```
1. main에서 브랜치 생성
   $ git checkout -b feat/PM-8-order-aggregate

2. 작업 후 커밋 (Conventional Commits)
   $ git commit -m "feat(order): Order Aggregate Root 및 상태 전이 규칙 구현 (#PM-8)"

3. Push 후 PR 생성
   $ git push -u origin feat/PM-8-order-aggregate
   → PR 제목: [Feat] Order Aggregate Root 및 상태 전이 규칙 구현 (#PM-8)

4. CI 통과 + 리뷰 승인 → Squash and Merge

5. 원격 브랜치 자동 삭제, main에서 다음 브랜치 생성
```

---

## 9. 단계별 구현 계획 (Issue 단위)

각 Issue는 하나의 브랜치(`feat/PM-{번호}-...`)와 하나의 PR에 대응한다.
Issue 간 의존 관계가 있으면 선행 Issue 번호를 `depends on`으로 명시한다.

**Issue별 작업 플로우**:
```
1. main에서 브랜치 생성       $ git checkout -b feat/PM-{번호}-short-description
2. 구현 + 테스트 작성          (완료 기준 충족까지)
3. 컨벤션에 맞춰 커밋          $ git commit -m "feat(scope): 설명 (#PM-{번호})"
4. Push + PR 생성             $ git push -u origin feat/PM-{번호}-...
                              → PR 제목: [Type] 설명 (#PM-{번호})
                              → PR 본문: Motivation / Modifications / Testing 템플릿
5. CI 통과 + 리뷰 승인        (빌드 + 테스트 + ArchUnit 통과 필수)
6. Squash and Merge → main   (원격 브랜치 자동 삭제)
7. 다음 Issue 브랜치 생성      (depends on 선행 Issue가 merge된 후)
```

> 모든 PM Issue는 위 플로우를 따른다. 구현 완료 후 반드시 PR을 생성하고, CI 통과 + 리뷰 승인 후 main에 merge한다.

---

### Phase 1: 프로젝트 기반 구축

> 목표: Gradle Multi-Module, Docker Compose 인프라, 공통 인프라(Outbox, 멱등성, 분산 락), ArchUnit 규칙 확립

#### PM-1. Gradle Multi-Module 프로젝트 초기 설정
- **Branch**: `chore/PM-1-gradle-multi-module`
- **내용**:
  - `.gitignore` 생성 (Gradle, Java, IDE, Docker, OS 아티팩트 제외)
  - Root `build.gradle` + `settings.gradle` 작성
  - `gradle/libs.versions.toml` Version Catalog 설정
  - 11개 모듈 생성: `pickme-common`, `pickme-order`, `pickme-payment`, `pickme-product`, `pickme-inventory`, `pickme-member`, `pickme-partner`, `pickme-notification`, `pickme-settlement`, `pickme-app`, `pickme-archunit`
  - 각 모듈 `build.gradle` 의존성 정의
  - `pickme-app`이 모든 모듈을 통합하여 단일 Spring Boot JAR 생성
  - Spring Boot 3.x + Java 21 기본 설정, `application.yml` 프로필별 분리 (local, docker, prod)
- **완료 기준**: `./gradlew clean build` 성공, `pickme-app` 모듈에서 Spring Boot 기동 확인, `.gitignore`로 빌드 아티팩트 미추적 확인

#### PM-2. Docker Compose 인프라 구성
- **Branch**: `chore/PM-2-docker-compose-infra`
- **depends on**: PM-1
- **내용**:
  - `docker-compose.infra.yml` 작성: PostgreSQL 16, Redis 7, Kafka (KRaft), Kafka UI, Zipkin
  - `docker-compose.yml` 작성: 인프라 + 애플리케이션 통합 실행
  - 애플리케이션 `Dockerfile` 작성 (멀티스테이지 빌드, Gradle 의존성 레이어 캐싱)
  - `infra/init-schemas.sql`: 8개 Schema-per-Module 자동 생성
  - Kafka 토픽 초기화 컨테이너 (`kafka-init`): 모듈별 8개 이벤트 토픽 + DLT 자동 생성
  - `.env.docker` 환경 변수 파일
- **완료 기준**: `docker compose -f docker-compose.infra.yml up -d` → 모든 서비스 healthy, `docker compose up -d --build` → 앱 포함 전체 기동

#### PM-3. Flyway DB 마이그레이션 설정
- **Branch**: `chore/PM-3-flyway-migration`
- **depends on**: PM-1, PM-2
- **내용**:
  - Flyway 의존성 추가 및 설정 (`pickme-app/src/main/resources/db/migration/`)
  - `V1__init_schemas.sql`: 8개 스키마 생성
  - `V2__create_outbox_and_processed_events.sql`: 모듈별 `outbox_events`, `processed_events` 테이블 생성 (8개 스키마 × 2 테이블)
  - `application.yml` Flyway 설정 (프로필별 DB 접속 정보)
- **완료 기준**: 앱 기동 시 Flyway 마이그레이션 자동 실행, `\dt order_schema.*` 등으로 테이블 존재 확인

#### PM-4. ArchUnit 테스트 — 모듈 경계 및 도메인 순수성
- **Branch**: `test/PM-4-archunit-rules`
- **depends on**: PM-1
- **내용**:
  - `pickme-archunit` 모듈에 ArchUnit 의존성 추가
  - `ModuleBoundaryTest`: 모듈 간 직접 import 금지 규칙 (order ↛ payment, product ↛ inventory 등 8개 모듈 조합)
  - `DomainPurityTest`: `domain` 패키지 → `infrastructure`, `api`, `org.springframework` import 금지
  - `NamingConventionTest`: Controller는 `*Controller`, Service는 `*Service`, Repository는 `*Repository` 접미사 강제
  - CI(`./gradlew test`)에서 위반 시 빌드 실패
- **완료 기준**: 의도적으로 위반하는 코드 작성 → ArchUnit 테스트 실패 확인 → 제거 후 통과

#### PM-5. GitHub Actions CI 파이프라인
- **Branch**: `ci/PM-5-github-actions`
- **depends on**: PM-1, PM-4
- **내용**:
  - `.github/workflows/ci.yml`: main PR 트리거, Java 21, Gradle build + test (ArchUnit 포함)
  - `.github/PULL_REQUEST_TEMPLATE.md`: PR 템플릿 설정
  - Branch Protection Rule 설정 가이드 문서 (main 직접 push 금지, CI 통과 필수, 리뷰 1명)
- **완료 기준**: PR 생성 시 CI 자동 실행, 빌드+테스트 통과 확인

#### PM-6. 공통 모듈 — Outbox 인프라
- **Branch**: `feat/PM-6-outbox-infra`
- **depends on**: PM-1, PM-3
- **내용**:
  - `pickme-common`에 Outbox 공통 코드 구현
  - `OutboxEvent` Entity: eventId, aggregateType, aggregateId, eventType, payload(JSONB), published, retryCount
  - `OutboxRepository` Interface + JPA 구현체
  - `OutboxRelayScheduler`: `@Scheduled`(500ms) Polling Publisher → Kafka 발행 → published=true 업데이트
  - Kafka Producer 설정 (`acks=all`, `enable.idempotence=true`)
- **완료 기준**: Outbox에 이벤트 INSERT → 500ms 이내 Kafka 토픽에 메시지 발행 확인 (Kafka UI)

#### PM-7. 공통 모듈 — 멱등성 필터 + 분산 락
- **Branch**: `feat/PM-7-idempotency-distributed-lock`
- **depends on**: PM-1, PM-3
- **내용**:
  - `ProcessedEvent` Entity + Repository: eventId 기반 중복 체크
  - `IdempotencyFilter`: Kafka Consumer에서 이벤트 수신 시 자동 중복 체크 (AOP 또는 공통 추상 Consumer)
  - `DistributedLockService`: Redisson 기반 분산 락 (`lock:{module}:{aggregate}:{id}`, TTL 5초, waitTime 3초)
  - 분산 락 AOP(`@DistributedLock`) 어노테이션 구현
- **완료 기준**: 동일 eventId 2회 전송 → 1회만 처리 확인. 동시 요청 시 분산 락으로 직렬화 확인

---

### Phase 2: 상품 / 재고 도메인

> 목표: Product, Inventory Aggregate 구현, 이벤트 연동, Redis 캐시

#### PM-8. Product Aggregate 및 도메인 모델
- **Branch**: `feat/PM-8-product-aggregate`
- **depends on**: PM-6
- **내용**:
  - `Product` Aggregate Root: productId(ProductId VO), productName(ProductName VO), price(ProductPrice VO), category(Category VO), status(ProductStatus), options(List\<ProductOption\>)
  - 상태 전이 규칙 캡슐화: `DRAFT → ON_SALE → SOLD_OUT → HIDDEN → DISCONTINUED`
  - `ProductRegisteredEvent`, `ProductInfoChangedEvent`, `ProductPriceChangedEvent` 도메인 이벤트 정의
  - Domain 패키지 순수성 (infrastructure/api 무의존)
  - 단위 테스트: VO 유효성 검증, 상태 전이 규칙 테스트
- **완료 기준**: 단위 테스트 통과, ArchUnit 도메인 순수성 테스트 통과

#### PM-9. Product CRUD API + Outbox 이벤트 발행
- **Branch**: `feat/PM-9-product-api`
- **depends on**: PM-8
- **내용**:
  - `ProductController`: `POST /api/v1/products`, `GET /api/v1/products/{id}`, `PATCH /api/v1/products/{id}`, `GET /api/v1/products`
  - `ProductService` (Application Layer): Command 처리, Outbox 이벤트 발행
  - `JpaProductRepository`: JPA 구현체, `product_schema` 접근
  - Flyway `V3__create_product_tables.sql`: `products`, `product_options`, `categories` 테이블
  - Request/Response DTO
  - API 통합 테스트 (`@WebMvcTest` + `@SpringBootTest`)
- **완료 기준**: 상품 등록 → Kafka `pickme.product.events` 토픽에 `ProductRegisteredEvent` 발행 확인

#### PM-10. Stock Aggregate 및 재고 연산
- **Branch**: `feat/PM-10-stock-aggregate`
- **depends on**: PM-7
- **내용**:
  - `Stock` Aggregate Root: stockId, productId, quantity(Quantity VO), reservedQuantity, totalQuantity
  - 재고 연산 캡슐화: `reserve()`, `confirm()`, `cancel()`, `restock()` — 모두 Quantity VO의 유효성 검증 포함
  - `InventoryReservedEvent`, `InventoryShortageEvent`, `InventoryRestoredEvent`, `StockDepletedEvent` 정의
  - Redisson 분산 락 적용: `lock:inventory:stock:{productId}`
  - Flyway `V4__create_inventory_tables.sql`: `stocks`, `stock_history`
  - 단위 테스트: 재고 연산 정합성, 음수 재고 방지, 분산 락 통합 테스트
- **완료 기준**: `reserve(5)` → quantity 감소, reservedQuantity 증가 확인. 동시 10건 요청 시 정합성 보장

#### PM-11. Product → Inventory 이벤트 연동
- **Branch**: `feat/PM-11-product-inventory-event`
- **depends on**: PM-9, PM-10
- **내용**:
  - `InventoryEventHandler`: `ProductRegisteredEvent` 구독 → 초기 `Stock` Aggregate 자동 생성
  - Kafka Consumer 구현 (`pickme.product.events` 토픽 구독)
  - 멱등성 필터 적용 (동일 이벤트 중복 처리 방지)
  - 통합 테스트: 상품 등록 → Stock 자동 생성 E2E 확인
- **완료 기준**: 상품 등록 API 호출 → Kafka → Inventory 모듈에서 Stock 생성 확인

#### PM-12. 상품 Redis 캐시
- **Branch**: `feat/PM-12-product-redis-cache`
- **depends on**: PM-9
- **내용**:
  - Cache Aside 패턴: 상품 상세 조회 시 Redis 확인 → Miss 시 DB 조회 + 캐시 저장
  - Key: `product:{id}`, TTL: 5분 + Jitter
  - `ProductInfoChangedEvent` 구독 시 캐시 무효화 (`cache eviction`)
  - Null Object Cache: 존재하지 않는 상품도 짧은 TTL(1분)로 캐시
  - 캐시 통합 테스트
- **완료 기준**: 상품 조회 → 캐시 히트 확인, 상품 수정 → 캐시 무효화 확인

---

### Phase 3: 회원 / 인증 도메인

> 목표: Member Aggregate, JWT 인증, 회원 이벤트 발행

#### PM-13. Member Aggregate 및 도메인 모델
- **Branch**: `feat/PM-13-member-aggregate`
- **depends on**: PM-6
- **내용**:
  - `Member` Aggregate Root: memberId, email(Email VO), password(Password VO), name(MemberName VO), phone(PhoneNumber VO), grade(MemberGrade), status(MemberStatus)
  - Value Object 유효성 검증: 이메일 형식, 전화번호 `010-XXXX-XXXX`, 이름 2~50자
  - 등급 재계산 로직: 누적 구매액 기준 `NORMAL → SILVER → GOLD → VIP → VVIP`
  - `MemberRegisteredEvent`, `MemberGradeChangedEvent` 정의
  - 단위 테스트
- **완료 기준**: VO 검증 테스트, 등급 재계산 로직 테스트 통과

#### PM-14. 회원 API + JWT 인증
- **Branch**: `feat/PM-14-member-api-jwt`
- **depends on**: PM-13
- **내용**:
  - `AuthController`: `POST /api/v1/auth/signup`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`
  - `MemberController`: `GET /api/v1/members/{id}`, `PATCH /api/v1/members/{id}`
  - Spring Security + JWT 토큰 발행/검증
  - Password BCrypt 해싱
  - Flyway `V5__create_member_tables.sql`: `members`, `member_auth`, `purchase_accumulation`
  - MemberRegisteredEvent Outbox 발행
  - Rate Limiter: 로그인 시도 IP+이메일당 5회/5분
  - API 통합 테스트
- **완료 기준**: 회원가입 → 로그인 → JWT 토큰 → 인증 필요 API 접근 성공

---

### Phase 4: 주문 도메인

> 목표: Order Aggregate, CQRS Read Model, Saga 이벤트 구독

#### PM-15. Order Aggregate 및 상태 전이 규칙
- **Branch**: `feat/PM-15-order-aggregate`
- **depends on**: PM-6
- **내용**:
  - `Order` Aggregate Root: orderId(OrderId VO), orderLines(List\<OrderLine\>), status(OrderStatus), shippingInfo(ShippingInfo VO), totalAmount(Money VO)
  - 상태 전이 캡슐화: `place()`, `confirm()`, `cancel()`, `requestRefund()`, `ship()`, `deliver()` — 각 메서드가 현재 상태 검증 후 전이
  - `OrderPlacedEvent`, `OrderConfirmedEvent`, `OrderCancelledEvent`, `OrderRefundRequestedEvent` 정의
  - 단위 테스트: 상태 전이 규칙 전체 경우의 수, 잘못된 전이 시 예외
- **완료 기준**: 모든 상태 전이 경로 테스트 통과 (정상 + 예외)

#### PM-16. 주문 생성 API + OrderPlacedEvent 발행
- **Branch**: `feat/PM-16-order-api`
- **depends on**: PM-15
- **내용**:
  - `OrderController`: `POST /api/v1/orders`, `GET /api/v1/orders/{id}`, `GET /api/v1/orders`, `PATCH /api/v1/orders/{id}/cancel`
  - `OrderService`: 주문 생성 → Outbox에 OrderPlacedEvent INSERT (같은 트랜잭션)
  - Flyway `V6__create_order_tables.sql`: `orders`, `order_lines`
  - JWT 인증 필수 (로그인한 회원만 주문 가능)
  - API 통합 테스트
- **완료 기준**: 주문 생성 → Kafka `pickme.order.events` 토픽에 OrderPlacedEvent 발행 확인

#### PM-17. CQRS Read Model — product_snapshot, member_snapshot
- **Branch**: `feat/PM-17-order-cqrs-read-model`
- **depends on**: PM-16, PM-9, PM-14
- **내용**:
  - `order_schema.product_snapshot`: 상품ID, 상품명, 가격 — `ProductRegisteredEvent`, `ProductInfoChangedEvent` 구독하여 갱신
  - `order_schema.member_snapshot`: 회원ID, 이름, 등급 — `MemberRegisteredEvent`, `MemberGradeChangedEvent` 구독하여 갱신
  - Flyway `V7__create_order_snapshots.sql`
  - 주문 생성 시 스냅샷에서 상품명/회원명 조회 (Cross-Module JOIN 대신)
  - 통합 테스트: 상품 등록 → 스냅샷 생성 확인, 상품 수정 → 스냅샷 갱신 확인
- **완료 기준**: 상품/회원 이벤트 → Read Model 반영 확인, 주문 생성 시 스냅샷 데이터 사용 확인

#### PM-18. 주문 Saga 이벤트 구독 (주문 확정/취소)
- **Branch**: `feat/PM-18-order-saga-consumer`
- **depends on**: PM-16
- **내용**:
  - `OrderEventHandler`:
    - `PaymentCompletedEvent` 구독 → `order.confirm()` → 상태 PAID
    - `PaymentFailedEvent` 구독 → `order.cancel()` → 상태 CANCELLED (보상)
    - `InventoryShortageEvent` 구독 → `order.cancel()` → 상태 CANCELLED (보상)
  - 멱등성 필터 적용
  - 보상 시 `OrderCancelledEvent` Outbox 발행 (Inventory 재고 복원 트리거)
  - 통합 테스트: PaymentCompletedEvent 수신 → 주문 상태 PAID 확인
- **완료 기준**: Saga 정상/보상 플로우 테스트 모두 통과

---

### Phase 5: 결제 도메인

> 목표: Payment Aggregate, PG사 ACL, Circuit Breaker

#### PM-19. Payment Aggregate 및 도메인 모델
- **Branch**: `feat/PM-19-payment-aggregate`
- **depends on**: PM-6
- **내용**:
  - `Payment` Aggregate Root: paymentId, orderId, payerId, amount(Money VO), paymentMethod(PaymentMethod), status(PaymentStatus), pgTransactionId
  - 상태 전이: `REQUESTED → PROCESSING → COMPLETED / FAILED → REFUND_REQUESTED → REFUNDED`
  - `PaymentCompletedEvent`, `PaymentFailedEvent`, `RefundCompletedEvent` 정의
  - Flyway `V8__create_payment_tables.sql`: `payments`, `payment_history`
  - 단위 테스트
- **완료 기준**: 상태 전이 테스트 통과

#### PM-20. 결제 처리 + PG사 ACL + OrderPlacedEvent 구독
- **Branch**: `feat/PM-20-payment-processing`
- **depends on**: PM-19, PM-16
- **내용**:
  - `PaymentEventHandler`: `OrderPlacedEvent` 구독 → Payment Aggregate 생성 → PG 승인 요청
  - `PgPaymentAdapter` (ACL): PG사 API 응답 → 내부 `PgResponse` VO 변환
  - PG사 Mock 구현 (Phase 1에서는 실제 PG 대신 Mock)
  - 결제 성공 → `PaymentCompletedEvent` Outbox 발행
  - 결제 실패 → `PaymentFailedEvent` Outbox 발행
  - Resilience4j Circuit Breaker 적용 (PG API)
  - 멱등성 필터 적용
  - 통합 테스트
- **완료 기준**: OrderPlacedEvent → Payment 생성 → PG 호출 → PaymentCompletedEvent 발행 확인

#### PM-21. 환불 처리
- **Branch**: `feat/PM-21-refund-processing`
- **depends on**: PM-20
- **내용**:
  - `OrderRefundRequestedEvent` 구독 → Payment 환불 처리
  - PG사 환불 API 호출 (ACL 경유)
  - `RefundCompletedEvent` Outbox 발행
  - 통합 테스트
- **완료 기준**: 환불 요청 → PG 환불 → RefundCompletedEvent 발행 확인

---

### Phase 6: 재고 Saga 연동 + E2E 통합 테스트

> 목표: Inventory ↔ Order Saga 완성, 전체 주문 플로우 E2E

#### PM-22. Inventory Saga 이벤트 구독
- **Branch**: `feat/PM-22-inventory-saga-consumer`
- **depends on**: PM-10, PM-16
- **내용**:
  - `InventoryEventHandler`:
    - `OrderPlacedEvent` 구독 → `stock.reserve()` (분산 락 적용) → 성공 시 `InventoryReservedEvent` / 실패 시 `InventoryShortageEvent`
    - `OrderConfirmedEvent` 구독 → `stock.confirm()` (실출고 확정)
    - `OrderCancelledEvent` 구독 → `stock.cancel()` (보상: 재고 복원) → `InventoryRestoredEvent`
  - 멱등성 필터 적용
  - 통합 테스트
- **완료 기준**: 주문 취소 → 재고 복원 확인, 재고 부족 → InventoryShortageEvent 발행 확인

#### PM-23. E2E 주문 플로우 통합 테스트
- **Branch**: `test/PM-23-e2e-order-flow`
- **depends on**: PM-18, PM-20, PM-22
- **내용**:
  - **정상 플로우**: 주문 생성 → 재고 예약 → 결제 요청 → 결제 완료 → 주문 확정 → 재고 확정
  - **보상 플로우 (결제 실패)**: 주문 생성 → 재고 예약 → 결제 실패 → 주문 취소 → 재고 복원
  - **보상 플로우 (재고 부족)**: 주문 생성 → 재고 부족 → 주문 실패
  - Docker Compose 기반 통합 테스트 환경
  - 각 시나리오에서 최종 DB 상태 + 이벤트 발행 이력 검증
- **완료 기준**: 3가지 시나리오 E2E 테스트 모두 통과

---

### Phase 7: 알림 / 파트너 / 정산 기본

> 목표: Notification, Partner, Settlement 기본 구현

#### PM-24. Notification 모듈 기본 구현
- **Branch**: `feat/PM-24-notification-module`
- **depends on**: PM-6
- **내용**:
  - `Notification` Aggregate, `NotificationChannel`, `SendStatus` VO
  - 이메일 발송 기본 구현 (Spring Mail)
  - 주요 이벤트 구독: `OrderPlacedEvent`(주문 접수), `PaymentCompletedEvent`(결제 완료), `MemberRegisteredEvent`(가입 환영)
  - Flyway `V9__create_notification_tables.sql`
  - 통합 테스트
- **완료 기준**: 주문 생성 → 주문 접수 알림 이메일 발송 확인

#### PM-25. Partner 모듈 기본 구현 + ACL 구조
- **Branch**: `feat/PM-25-partner-module`
- **depends on**: PM-6
- **내용**:
  - `Partner` Aggregate, `BusinessInfo`, `ContractInfo`, `PartnerStatus` VO
  - Partner CRUD API: `POST /api/v1/partners`, `GET /api/v1/partners/{id}`
  - ACL 어댑터 인터페이스 정의: `PgPaymentGateway`, `DeliveryGateway`, `NotificationGateway`
  - Flyway `V10__create_partner_tables.sql`
  - 통합 테스트
- **완료 기준**: 파트너 등록/조회 API 동작 확인

#### PM-26. Settlement 모듈 기본 구현
- **Branch**: `feat/PM-26-settlement-module`
- **depends on**: PM-6, PM-20
- **내용**:
  - `Settlement` Aggregate, `SettlementPeriod`, `CommissionRate`, `SettlementStatus` VO
  - `PaymentCompletedEvent` 구독 → `settlement_schema.sales_snapshot` 매출 누적
  - `RefundCompletedEvent` 구독 → 환불 내역 반영
  - `PartnerApprovedEvent` 구독 → 파트너 정산 정보 스냅샷
  - Flyway `V11__create_settlement_tables.sql`
  - 통합 테스트
- **완료 기준**: 결제 완료 → 매출 스냅샷 누적 확인

#### PM-27. Kafka DLT 모니터링 + 재처리 API
- **Branch**: `feat/PM-27-dlt-monitoring`
- **depends on**: PM-6
- **내용**:
  - DLT Consumer: `pickme.dead-letter` 토픽 구독, 실패 이벤트 DB 저장
  - 재처리 API: `POST /api/v1/admin/dlt/{eventId}/retry`
  - DLT 적재 시 Slack Webhook 알림 (설정 가능)
  - 통합 테스트
- **완료 기준**: 의도적 실패 이벤트 → DLT 적재 → 재처리 API로 재시도 확인

---

### Phase 8: 성능 최적화 + 가용성 강화

> 목표: Rate Limiter, Timeout, Cache 고도화, 재고 Pre-deduction

#### PM-28. Rate Limiter 구현
- **Branch**: `feat/PM-28-rate-limiter`
- **depends on**: PM-7
- **내용**:
  - Redis Sliding Window Counter 기반 Rate Limiter
  - 주문 생성: 회원당 10 req/min
  - 결제 요청: 회원당 5 req/min
  - 로그인: IP+이메일당 5 req/5min
  - API 전체: IP당 100 req/min
  - `429 Too Many Requests` 응답 + `Retry-After` 헤더
  - 통합 테스트
- **완료 기준**: Rate Limit 초과 시 429 응답 확인

#### PM-29. Timeout 정책 전체 적용
- **Branch**: `feat/PM-29-timeout-policy`
- **depends on**: PM-1
- **내용**:
  - HikariCP `connectionTimeout: 3000ms`
  - Redis `commandTimeout: 1000ms`
  - 외부 API `connectTimeout: 3s`, `readTimeout: 5s`
  - Kafka Producer `delivery.timeout.ms: 3000`
  - 분산 락 TTL: 5s, waitTime: 3s
  - `application.yml`에 중앙 집중 관리
- **완료 기준**: 각 구간별 Timeout 설정 확인, 느린 외부 API 시뮬레이션 → Timeout 동작 확인

#### PM-30. Redis Cache 고도화
- **Branch**: `perf/PM-30-cache-optimization`
- **depends on**: PM-12
- **내용**:
  - TTL Jitter: `baseTimeout + random(0, baseTimeout * 0.1)`
  - Cache Warm-up: 앱 시작 시 인기 상품 Top N 사전 로딩 (`@PostConstruct` 또는 `ApplicationReadyEvent`)
  - Null Object Cache: 미존재 상품 캐시 (TTL 1분)
  - Cache Stampede 방지: 분산 락으로 단일 스레드만 DB 조회
  - 통합 테스트
- **완료 기준**: Cache Miss Storm 시뮬레이션 → DB 쿼리 1회만 발생 확인

#### PM-31. 재고 Pre-deduction (Redis Lua Script)
- **Branch**: `perf/PM-31-inventory-pre-deduction`
- **depends on**: PM-10, PM-7
- **내용**:
  - Redis에 재고 수량 캐시 (`stock:{productId}`, Write Through)
  - Lua Script로 원자적 재고 차감 (DB 락 회피)
  - Redis 차감 성공 → DB 비동기 반영
  - Redis 차감 실패(재고 부족) → 즉시 응답 (DB 접근 안 함)
  - 입고/복원 시 Redis 수량 동기 갱신
  - 통합 테스트: 동시 100건 재고 차감 → 정합성 확인
- **완료 기준**: 동시 요청 시 DB 락 없이 정합성 보장, 응답 시간 개선 확인

---

### Phase 9: 모니터링 + 정산 고도화

> 목표: Prometheus/Grafana, 분산 트레이싱, 정산 배치

#### PM-32. Micrometer + Prometheus + Grafana 모니터링
- **Branch**: `feat/PM-32-monitoring-stack`
- **depends on**: PM-2
- **내용**:
  - Spring Boot Actuator + Micrometer Prometheus 메트릭 노출
  - `docker-compose.infra.yml`에 Prometheus, Grafana 추가
  - Grafana 대시보드: JVM 메트릭, HTTP 요청, Kafka Consumer Lag, Redis 히트율, DB 커넥션 풀
  - 커스텀 메트릭: 주문 TPS, 결제 성공률, 재고 차감 지연 시간
- **완료 기준**: Grafana 대시보드에서 주요 메트릭 시각화 확인

#### PM-33. 분산 트레이싱 + correlationId
- **Branch**: `feat/PM-33-distributed-tracing`
- **depends on**: PM-2
- **내용**:
  - Micrometer Tracing + Zipkin 연동
  - `correlationId` (traceId) HTTP 요청 → Kafka 이벤트 → Consumer까지 전파
  - 이벤트 Envelope에 `traceId` 필드 추가
  - Zipkin UI에서 주문 플로우 전체 추적 확인
- **완료 기준**: 주문 생성 → 재고 예약 → 결제 → 주문 확정 전체 흐름이 Zipkin에서 단일 trace로 추적 가능

#### PM-34. Settlement 정산 배치 + ETL Aggregate Table
- **Branch**: `feat/PM-34-settlement-batch`
- **depends on**: PM-26
- **내용**:
  - `daily_sales_aggregate` 테이블: 일별 파트너별 매출 집계
  - 실시간 누적: PaymentCompletedEvent/RefundCompletedEvent 수신 시 UPSERT
  - 일 1회 Reconciliation 배치: 원천 데이터와 Aggregate 정합성 검증
  - 정산 API: `GET /api/v1/settlements`, `GET /api/v1/settlements/{id}`
  - 통합 테스트
- **완료 기준**: 매출 집계 정확성 확인, Reconciliation 불일치 감지 확인

#### PM-35. 정합성 검증 배치
- **Branch**: `feat/PM-35-consistency-check-batch`
- **depends on**: PM-23
- **내용**:
  - 일 1회 배치: 주문-결제-재고 정합성 검증
  - 불일치 감지 시 알림 (Slack) + 수동 보정 API
  - 좀비 주문 감지 (PAYMENT_PENDING 상태 N시간 이상 유지)
  - 통합 테스트
- **완료 기준**: 의도적 불일치 상황 → 배치에서 감지 → 알림 확인

---

### Phase 10: DB 최적화 + Noisy Neighbor 대응

> 목표: Read Replica, 파티셔닝, DataSource 분리

#### PM-36. DB Read Replica 라우팅
- **Branch**: `feat/PM-36-read-replica-routing`
- **depends on**: PM-1
- **내용**:
  - `AbstractRoutingDataSource` 기반 Read/Write 라우팅
  - `@Transactional(readOnly = true)` → Replica로 자동 라우팅
  - Docker Compose에 PostgreSQL Replica 추가 (스트리밍 복제)
  - 통합 테스트
- **완료 기준**: readOnly 트랜잭션 → Replica 접근 확인

#### PM-37. orders 테이블 파티셔닝
- **Branch**: `perf/PM-37-order-table-partitioning`
- **depends on**: PM-16
- **내용**:
  - `orders` 테이블 월별 Range Partitioning (`ordered_at` 기준)
  - Flyway 마이그레이션으로 파티션 전환
  - 파티션 프루닝 확인 (EXPLAIN ANALYZE)
- **완료 기준**: 날짜 범위 조회 시 해당 파티션만 스캔 확인

#### PM-38. 모듈별 독립 DataSource (Noisy Neighbor 대응)
- **Branch**: `feat/PM-38-per-module-datasource`
- **depends on**: PM-1
- **내용**:
  - 모듈별 독립 `EntityManagerFactory` + `DataSource` 설정
  - 커넥션 풀 분리: 주문 10개, 결제 10개, 재고 10개, 나머지 각 5개
  - 한 모듈의 Heavy Query가 다른 모듈 커넥션 풀에 영향 안 줌
- **완료 기준**: 한 모듈에서 의도적 슬로우 쿼리 실행 → 다른 모듈 응답 시간 영향 없음 확인

---

### Phase 11: MSA 전환 준비 (트래픽 기준 도달 시)

> 목표: CDC 고도화, 병목 모듈 분리, API Gateway

#### PM-39. Debezium CDC 도입
- **Branch**: `feat/PM-39-debezium-cdc`
- **내용**:
  - Debezium + Kafka Connect 설정
  - Outbox 테이블 CDC → Kafka 자동 발행 (Polling Publisher 대체)
  - CDC + CQRS Read Model 동기화
  - Docker Compose에 Debezium, Kafka Connect 추가

#### PM-40. 병목 모듈 독립 서비스화 (Order)
- **Branch**: `feat/PM-40-order-service-extraction`
- **내용**:
  - Order 모듈 독립 Spring Boot 애플리케이션으로 분리
  - 독립 DB (`order_db`), 독립 Dockerfile, 독립 `docker-compose` 서비스
  - Kafka 기반 통신 유지 (코드 변경 최소)

#### PM-41. 병목 모듈 독립 서비스화 (Payment, Inventory)
- **Branch**: `feat/PM-41-payment-inventory-extraction`
- **depends on**: PM-40
- **내용**: PM-40과 동일한 패턴으로 Payment, Inventory 분리

#### PM-42. API Gateway 도입
- **Branch**: `feat/PM-42-api-gateway`
- **depends on**: PM-40, PM-41
- **내용**:
  - Spring Cloud Gateway 도입
  - 라우팅 규칙: `/api/v1/orders/**` → Order Service, `/api/v1/payments/**` → Payment Service
  - JWT 토큰 검증 Gateway 레벨에서 처리
  - Docker Compose에 Gateway 추가

---

## 10. 프로젝트 구조

### 10.1 Gradle Multi-Module 구조

```
pick-me/
├── .gitignore                      # Gradle/Java/IDE/Docker/OS 아티팩트 제외
├── .editorconfig                   # 파일별 포맷 규칙
├── .gitmessage                     # 커밋 메시지 템플릿
├── .github/
│   └── PULL_REQUEST_TEMPLATE.md    # PR 본문 템플릿
├── build.gradle                    # Root build 설정
├── settings.gradle                 # 모듈 include
├── gradle/
│   └── libs.versions.toml          # Version Catalog
├── PRD.md                          # 이 문서
├── Dockerfile                      # 애플리케이션 멀티스테이지 빌드
├── docker-compose.yml              # 전체 인프라 + 앱 통합 실행
├── docker-compose.infra.yml        # 인프라만 실행 (로컬 개발용)
├── .env.docker                     # Docker 환경 변수
├── infra/
│   └── init-schemas.sql            # Schema-per-Module 초기화
│
├── pickme-common/                  # 공통 모듈
│   ├── build.gradle
│   └── src/main/java/com/pickme/common/
│       ├── event/                  # Event Envelope, DomainEvent interface
│       │   ├── DomainEvent.java
│       │   ├── EventEnvelope.java
│       │   └── EventType.java
│       ├── model/                  # 공통 VO (Money 등)
│       │   └── Money.java
│       ├── outbox/                 # Outbox 공통 인프라
│       │   ├── OutboxEvent.java
│       │   ├── OutboxRepository.java
│       │   └── OutboxRelayScheduler.java
│       ├── idempotency/            # 멱등성 공통 인프라
│       │   ├── ProcessedEvent.java
│       │   └── IdempotencyFilter.java
│       └── lock/                   # 분산 락
│           └── DistributedLockService.java
│
├── pickme-order/                   # 주문 모듈
│   ├── build.gradle
│   └── src/main/java/com/pickme/order/
│       ├── api/
│       │   ├── OrderController.java
│       │   ├── request/
│       │   └── response/
│       ├── application/
│       │   ├── OrderService.java
│       │   ├── OrderEventHandler.java      # 외부 이벤트 수신 처리
│       │   └── command/
│       ├── domain/
│       │   ├── model/
│       │   │   ├── Order.java              # Aggregate Root
│       │   │   ├── OrderLine.java
│       │   │   ├── OrderId.java            # Value Object
│       │   │   ├── OrderStatus.java        # Value Object (Enum)
│       │   │   ├── ShippingInfo.java        # Value Object
│       │   │   └── Address.java            # Value Object
│       │   ├── event/
│       │   │   ├── OrderPlacedEvent.java
│       │   │   ├── OrderConfirmedEvent.java
│       │   │   └── OrderCancelledEvent.java
│       │   ├── repository/
│       │   │   └── OrderRepository.java    # Interface (Port)
│       │   └── service/
│       │       └── OrderDomainService.java
│       └── infrastructure/
│           ├── persistence/
│           │   ├── JpaOrderRepository.java
│           │   ├── OrderJpaEntity.java     # JPA Entity (domain 모델과 분리)
│           │   └── OrderMapper.java        # Domain ↔ JPA 변환
│           ├── messaging/
│           │   ├── OrderEventPublisher.java
│           │   └── OrderEventConsumer.java
│           ├── snapshot/
│           │   ├── ProductSnapshot.java    # CQRS Read Model
│           │   └── MemberSnapshot.java
│           └── config/
│               └── OrderModuleConfig.java
│
├── pickme-payment/                 # 결제 모듈 (동일 패키지 구조)
├── pickme-product/                 # 상품 모듈
├── pickme-inventory/               # 재고 모듈
├── pickme-member/                  # 회원 모듈
├── pickme-partner/                 # 파트너 모듈 (ACL 구현 포함)
├── pickme-notification/            # 알림 모듈
├── pickme-settlement/              # 정산 모듈
│
├── pickme-app/                     # 실행 모듈 (Phase 1: 단일 Spring Boot App)
│   ├── build.gradle                # 모든 모듈 의존성 포함
│   └── src/main/
│       ├── java/com/pickme/PickMeApplication.java
│       └── resources/
│           ├── application.yml
│           ├── application-local.yml
│           ├── application-prod.yml
│           └── db/migration/
│               ├── V1__init_schemas.sql
│               └── V2__create_tables.sql
│
└── pickme-archunit/                # ArchUnit 테스트 전용 모듈
    └── src/test/java/com/pickme/archunit/
        ├── ModuleBoundaryTest.java     # 모듈 간 참조 금지
        ├── DomainPurityTest.java       # domain → infra/api 참조 금지
        └── NamingConventionTest.java   # 네이밍 규칙
```

### 10.2 핵심 ArchUnit 규칙

```java
// ModuleBoundaryTest.java — 모듈 간 직접 참조 금지
@ArchTest
static final ArchRule 주문모듈은_결제모듈_내부를_참조하지_않는다 =
    noClasses()
        .that().resideInAPackage("..order..")
        .should().dependOnClassesThat()
        .resideInAPackage("..payment.internal..");

// DomainPurityTest.java — 도메인 패키지 순수성
@ArchTest
static final ArchRule 도메인은_인프라를_참조하지_않는다 =
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("..infrastructure..", "..api..");

@ArchTest
static final ArchRule 도메인은_스프링에_의존하지_않는다 =
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("org.springframework..");
```

### 10.3 Docker / Docker Compose 구성

모든 인프라와 애플리케이션을 `docker compose up` 한 번으로 실행할 수 있어야 한다. 로컬 개발, 통합 테스트, CI 환경 모두 동일한 Docker Compose를 기반으로 동작한다.

#### 10.3.1 인프라 구성 (`docker-compose.infra.yml`)

로컬 개발 시 인프라만 띄우고 애플리케이션은 IDE에서 직접 실행하는 용도.

```yaml
services:
  # ─── PostgreSQL ───
  postgres:
    image: postgres:16-alpine
    container_name: pickme-postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: pickme_db
      POSTGRES_USER: pickme
      POSTGRES_PASSWORD: pickme1234
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./infra/init-schemas.sql:/docker-entrypoint-initdb.d/01-init-schemas.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U pickme -d pickme_db"]
      interval: 5s
      timeout: 5s
      retries: 5

  # ─── Redis ───
  redis:
    image: redis:7-alpine
    container_name: pickme-redis
    ports:
      - "6379:6379"
    command: redis-server --requirepass pickme1234 --maxmemory 256mb --maxmemory-policy allkeys-lru
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "pickme1234", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5

  # ─── Kafka (KRaft, Zookeeper 없음) ───
  kafka:
    image: confluentinc/cp-kafka:7.6.0
    container_name: pickme-kafka
    ports:
      - "9092:9092"      # 외부 (호스트) 접근
      - "29092:29092"    # 내부 (Docker 네트워크) 접근
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:29092,CONTROLLER://0.0.0.0:9093,EXTERNAL://0.0.0.0:9092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,EXTERNAL://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT,EXTERNAL:PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
      CLUSTER_ID: "pickme-kafka-cluster-001"
    volumes:
      - kafka-data:/var/lib/kafka/data
    healthcheck:
      test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:9092"]
      interval: 10s
      timeout: 10s
      retries: 5

  # ─── Kafka 토픽 초기화 ───
  kafka-init:
    image: confluentinc/cp-kafka:7.6.0
    container_name: pickme-kafka-init
    depends_on:
      kafka:
        condition: service_healthy
    entrypoint: ["/bin/bash", "-c"]
    command: |
      "
      echo 'Creating Kafka topics...'

      # 도메인 이벤트 토픽 (모듈별)
      kafka-topics --create --if-not-exists --bootstrap-server kafka:29092 \
        --topic pickme.order.events --partitions 6 --replication-factor 1
      kafka-topics --create --if-not-exists --bootstrap-server kafka:29092 \
        --topic pickme.payment.events --partitions 6 --replication-factor 1
      kafka-topics --create --if-not-exists --bootstrap-server kafka:29092 \
        --topic pickme.product.events --partitions 3 --replication-factor 1
      kafka-topics --create --if-not-exists --bootstrap-server kafka:29092 \
        --topic pickme.inventory.events --partitions 6 --replication-factor 1
      kafka-topics --create --if-not-exists --bootstrap-server kafka:29092 \
        --topic pickme.member.events --partitions 3 --replication-factor 1
      kafka-topics --create --if-not-exists --bootstrap-server kafka:29092 \
        --topic pickme.partner.events --partitions 1 --replication-factor 1
      kafka-topics --create --if-not-exists --bootstrap-server kafka:29092 \
        --topic pickme.notification.events --partitions 3 --replication-factor 1
      kafka-topics --create --if-not-exists --bootstrap-server kafka:29092 \
        --topic pickme.settlement.events --partitions 1 --replication-factor 1

      # DLT (Dead Letter Topic)
      kafka-topics --create --if-not-exists --bootstrap-server kafka:29092 \
        --topic pickme.dead-letter --partitions 3 --replication-factor 1

      echo 'All topics created.'
      "

  # ─── Kafka UI (모니터링) ───
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: pickme-kafka-ui
    ports:
      - "8089:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: pickme-local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
    depends_on:
      kafka:
        condition: service_healthy

  # ─── Zipkin (분산 트레이싱) ───
  zipkin:
    image: openzipkin/zipkin:latest
    container_name: pickme-zipkin
    ports:
      - "9411:9411"

volumes:
  postgres-data:
  redis-data:
  kafka-data:
```

**PostgreSQL 스키마 초기화** (`infra/init-schemas.sql`):
```sql
-- Schema-per-Module 초기화 (docker-entrypoint-initdb.d에 의해 자동 실행)
CREATE SCHEMA IF NOT EXISTS order_schema;
CREATE SCHEMA IF NOT EXISTS payment_schema;
CREATE SCHEMA IF NOT EXISTS product_schema;
CREATE SCHEMA IF NOT EXISTS inventory_schema;
CREATE SCHEMA IF NOT EXISTS member_schema;
CREATE SCHEMA IF NOT EXISTS partner_schema;
CREATE SCHEMA IF NOT EXISTS notification_schema;
CREATE SCHEMA IF NOT EXISTS settlement_schema;
```

#### 10.3.2 애플리케이션 Dockerfile (멀티스테이지 빌드)

```dockerfile
# ─── Stage 1: Build ───
FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app
COPY build.gradle settings.gradle gradle/ ./
COPY gradle/ gradle/
# 의존성 캐싱 (소스 변경 시 레이어 캐시 활용)
COPY pickme-common/build.gradle pickme-common/
COPY pickme-order/build.gradle pickme-order/
COPY pickme-payment/build.gradle pickme-payment/
COPY pickme-product/build.gradle pickme-product/
COPY pickme-inventory/build.gradle pickme-inventory/
COPY pickme-member/build.gradle pickme-member/
COPY pickme-partner/build.gradle pickme-partner/
COPY pickme-notification/build.gradle pickme-notification/
COPY pickme-settlement/build.gradle pickme-settlement/
COPY pickme-app/build.gradle pickme-app/
RUN gradle dependencies --no-daemon || true
# 소스 복사 및 빌드
COPY . .
RUN gradle :pickme-app:bootJar --no-daemon -x test

# ─── Stage 2: Runtime ───
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S pickme && adduser -S pickme -G pickme
COPY --from=builder /app/pickme-app/build/libs/*.jar app.jar
USER pickme
EXPOSE 8080
ENTRYPOINT ["java", \
  "-XX:+UseZGC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
```

#### 10.3.3 통합 실행 (`docker-compose.yml`)

인프라 + 애플리케이션을 한 번에 실행. CI 환경 및 데모용.

```yaml
services:
  # 인프라 (docker-compose.infra.yml 내용 포함)
  postgres:
    # ... (infra.yml과 동일)
  redis:
    # ... (infra.yml과 동일)
  kafka:
    # ... (infra.yml과 동일)
  kafka-init:
    # ... (infra.yml과 동일)
  kafka-ui:
    # ... (infra.yml과 동일)
  zipkin:
    # ... (infra.yml과 동일)

  # ─── pick-me 애플리케이션 ───
  pickme-app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: pickme-app
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      # PostgreSQL
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/pickme_db
      SPRING_DATASOURCE_USERNAME: pickme
      SPRING_DATASOURCE_PASSWORD: pickme1234
      # Redis
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
      SPRING_DATA_REDIS_PASSWORD: pickme1234
      # Kafka
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      # Zipkin
      MANAGEMENT_ZIPKIN_TRACING_ENDPOINT: http://zipkin:9411/api/v2/spans
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      kafka:
        condition: service_healthy
      kafka-init:
        condition: service_completed_successfully
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 30s
```

#### 10.3.4 실행 명령어

```bash
# ─── 로컬 개발: 인프라만 실행 (앱은 IDE에서 실행) ───
docker compose -f docker-compose.infra.yml up -d

# ─── 전체 실행: 인프라 + 애플리케이션 ───
docker compose up -d --build

# ─── 상태 확인 ───
docker compose ps

# ─── 로그 확인 ───
docker compose logs -f pickme-app          # 애플리케이션 로그
docker compose logs -f kafka               # Kafka 로그

# ─── 종료 ───
docker compose down                        # 컨테이너 종료 (볼륨 유지)
docker compose down -v                     # 컨테이너 + 볼륨 삭제 (초기화)

# ─── Kafka 토픽 확인 ───
docker exec pickme-kafka kafka-topics --list --bootstrap-server localhost:9092

# ─── PostgreSQL 접속 ───
docker exec -it pickme-postgres psql -U pickme -d pickme_db

# ─── Redis 접속 ───
docker exec -it pickme-redis redis-cli -a pickme1234
```

#### 10.3.5 로컬 개발 접속 정보

| 서비스 | URL / Host | 비고 |
|--------|-----------|------|
| **PostgreSQL** | `localhost:5432` | DB: `pickme_db`, User: `pickme`, PW: `pickme1234` |
| **Redis** | `localhost:6379` | PW: `pickme1234` |
| **Kafka Broker** | `localhost:9092` | 호스트에서 접근 시 |
| **Kafka (Docker 내부)** | `kafka:29092` | 컨테이너 간 통신 |
| **Kafka UI** | `http://localhost:8089` | 토픽, Consumer Group, 메시지 모니터링 |
| **Zipkin** | `http://localhost:9411` | 분산 트레이싱 대시보드 |
| **pick-me App** | `http://localhost:8080` | Spring Boot API (`docker-compose.yml` 사용 시) |
| **Actuator Health** | `http://localhost:8080/actuator/health` | 헬스 체크 |

#### 10.3.6 Phase 3: MSA 전환 시 Docker Compose 확장

MSA로 분리되면 모듈별 독립 컨테이너 + 독립 DB로 확장한다.

```yaml
# docker-compose.msa.yml (Phase 3 구조 예시)
services:
  # 인프라 (동일)
  postgres-order:     # 주문 전용 DB
  postgres-payment:   # 결제 전용 DB
  postgres-inventory: # 재고 전용 DB
  redis:
  kafka:

  # 서비스별 독립 컨테이너
  pickme-order:
    build: ./pickme-order
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-order:5432/order_db
  pickme-payment:
    build: ./pickme-payment
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-payment:5432/payment_db
  pickme-inventory:
    build: ./pickme-inventory
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-inventory:5432/inventory_db

  # API Gateway
  api-gateway:
    build: ./pickme-gateway
    ports:
      - "8080:8080"
```

---

## 11. 기술 부채 및 리스크 관리

### 11.1 Known Trade-offs

| 결정 | Trade-off | 수용 근거 |
|------|-----------|----------|
| Kafka (Phase 1부터) | SQS 대비 운영 복잡도 높음 | AWS MSK로 운영 부담 최소화, 브로커 마이그레이션 비용 제로, 이벤트 리플레이/순서 보장 이점이 큼 |
| Schema-per-Module (단일 DB) | 완전한 DB 격리가 아님 (같은 인스턴스) | Phase 1에서 비용 효율적, Phase 3에서 DB 분리 |
| Choreography Saga | 분산 플로우 추적 어려움, Saga 상태 한눈에 보기 어려움 | correlationId + 분산 트레이싱으로 보완. Orchestrator 대비 결합도 낮음 |
| Eventual Consistency | 일시적 데이터 불일치 가능 | UX에서 "처리 중" 상태 표시로 사용자 기대 관리 |
| Read Model 복제 | 데이터 중복, 스냅샷 동기화 오버헤드 | Cross-Module JOIN 금지를 위한 필수 비용 |
| 모듈별 Money VO 복제 | DRY 원칙 위반 | 모듈 독립성 > DRY. 서비스 분리 시 의존성 없이 가져감 |

### 11.2 리스크 대응

| 리스크 | 영향 | 대응 |
|--------|------|------|
| 이벤트 유실 | 주문/결제/재고 불일치 | Outbox 패턴 + DLQ + 일 1회 정합성 배치 검증 |
| 보상 트랜잭션 실패 | 좀비 주문/미복원 재고 발생 | DLQ 모니터링 + 수동 개입 API + 정합성 체크 배치 |
| Redis 장애 | 캐시/분산 락/Rate Limiter 불가 | Redis Cluster HA + Fallback to DB 직접 조회 |
| Kafka 장애 | 이벤트 전달 중단 | Outbox에 이벤트 보존 → Kafka 복구 시 자동 재발행 (미발행분 Polling). MSK 멀티 AZ로 가용성 확보 |
| Schema-per-Module 한계 | 모듈 간 집계 쿼리 어려움 | ETL Aggregate Table + CQRS Read Model |
| 분산 트랜잭션 정합성 | Eventual Consistency 구간 데이터 불일치 | Saga 보상 + 정합성 배치 + "처리 중" UX |

### 11.3 Event Sourcing 도입 기준 (Phase 3+)

현재는 State-based(최종 상태 저장) 방식을 채택한다. Event Sourcing은 아래 조건 충족 시 선택적으로 도입한다.

> **최종 상태 관리 방식의 문제점**: UPDATE로 상태를 덮어쓰면 "왜 이 상태가 되었는지" 추적할 수 없다. 주문이 취소되었을 때, 결제 후 취소인지 결제 전 취소인지 상태만으로는 구분할 수 없다.

**도입 조건**:
- 주문/결제 이력의 **완전한 감사 추적(Audit Trail)** 이 법적/비즈니스 요건으로 필요한 경우
- 시점별 상태 복원(Temporal Query)이 필요한 경우 (예: "3월 15일 시점의 주문 상태는?")
- 이벤트 리플레이 기반 Read Model 재구축이 필요한 경우 (데이터 마이그레이션, 버그 수정)

**도입 시 구조**:
```
Event Store (append-only) ──CDC──→ Kafka ──→ Read Model Projector
                                          ──→ CQRS Query Side
```

- Event Store는 append-only로 이벤트를 저장 (UPDATE/DELETE 없음)
- Projection이 이벤트 스트림을 소비하여 Read Model을 구축
- CQRS와 자연스럽게 결합: Command → Event Store, Query → Read Model

---

**Last Updated**: 2026-04-02
