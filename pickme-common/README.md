# pickme-common (공통 인프라)

> Outbox 패턴, 멱등성 필터, 분산 락, Rate Limiter, 도메인 이벤트 공통

## 도메인 이벤트 인프라

| 클래스 | 역할 |
|--------|------|
| `DomainEvent` | 모든 도메인 이벤트의 인터페이스 (eventId, eventType, aggregateType, aggregateId, occurredAt) |
| `DomainEventProvider` | Aggregate Root가 구현하는 인터페이스 (getDomainEvents, clearDomainEvents) |
| `DomainEventPublisher` | Outbox 발행 단일 컴포넌트 — 7개 서비스에서 공유 |
| `EventEnvelope` | 이벤트 직렬화 래퍼 (traceId 포함) |

## Transactional Outbox

| 클래스 | 역할 |
|--------|------|
| `OutboxEvent` | Outbox JPA Entity (eventId, aggregateType, payload, published, retryCount) |
| `OutboxRepository` | 미발행 이벤트 조회 (published=false, retryCount < max) |
| `OutboxRelayScheduler` | 500ms 주기 Polling → Kafka 발행 → published=true |

## 멱등성

| 클래스 | 역할 |
|--------|------|
| `ProcessedEvent` | 처리 완료 이벤트 기록 (eventId PK) |
| `IdempotencyFilter` | isDuplicate() / markProcessed() |

## 분산 락

| 클래스 | 역할 |
|--------|------|
| `@DistributedLock` | AOP 어노테이션 (key, waitTime, leaseTime) |
| `DistributedLockAspect` | Redisson tryLock, SpEL 키 해석 |

## Rate Limiter

| 클래스 | 역할 |
|--------|------|
| `@RateLimiter` | AOP 어노테이션 (key, limit, windowSeconds) |
| `RateLimiterAspect` | Redis Lua Script Sliding Window Counter |
| `RateLimitExceptionHandler` | 429 + Retry-After 헤더 |

## DLT 모니터링

| 클래스 | 역할 |
|--------|------|
| `DeadLetterEvent` | DLT 이벤트 DB 저장 |
| `DeadLetterConsumer` | pickme.dead-letter 토픽 구독 |
| `DeadLetterAdminController` | GET /api/v1/admin/dlt, POST /{eventId}/retry |
| `SlackNotifier` | DLT 적재 시 Slack Webhook 알림 |

## 커스텀 메트릭

`BusinessMetrics` — 주문 TPS, 결제 성공/실패, 재고 차감 지연 시간
