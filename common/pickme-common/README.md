# pickme-common (공통 인프라)

> Outbox 패턴, Debezium CDC, 멱등성 필터, 분산 락, Rate Limiter, 도메인 이벤트 공통

## 도메인 이벤트 인프라

| 클래스 | 역할 |
|--------|------|
| `DomainEvent` | 모든 도메인 이벤트의 인터페이스 (eventId, eventType, aggregateType, aggregateId, occurredAt) |
| `DomainEventProvider` | Aggregate Root가 구현하는 인터페이스 (getDomainEvents, clearDomainEvents) |
| `DomainEventPublisher` | Outbox 발행 단일 컴포넌트 — 8개 서비스에서 공유 |
| `EventEnvelope` | 이벤트 직렬화 래퍼 (traceId 포함) |

## Transactional Outbox + CDC

도메인 이벤트의 신뢰성 있는 전파를 위해 Transactional Outbox 패턴을 사용합니다.
이벤트 전송 메커니즘은 **폴링 Relay → Debezium CDC(로그 기반)**로 마이그레이션되었습니다.

| 클래스 | 역할 |
|--------|------|
| `OutboxEvent` | Outbox JPA Entity (eventId, aggregateType, aggregateId, eventType, payload(JSONB), createdAt) |
| `OutboxRepository` | 미발행 이벤트 조회 |
| `OutboxRelayScheduler` | **[레거시]** 500ms 주기 Polling → Kafka 발행 (`pickme.outbox.relay.enabled`로 ON/OFF) |
| `OutboxCleanupScheduler` | 매일 04:00 UTC에 7일 이상 된 Outbox 이벤트 삭제 |

### 이벤트 전파 흐름

```
현재 (Debezium CDC):
  @Transactional { domainLogic() + eventPublisher.publishAll() }
    → OutboxEvent INSERT
    → PostgreSQL WAL 기록
    → Debezium Connector (pgoutput)
    → Outbox EventRouter SMT
    → Kafka Topics (pickme.{domain}.events)

레거시 (Polling Relay):
  OutboxRelayScheduler (500ms) → findUnpublished → Kafka send → markPublished
  설정: pickme.outbox.relay.enabled: false (비활성화)
```

### Debezium 전환으로 제거된 컬럼 (V16 마이그레이션)

폴링에 필요했던 `published`, `published_at`, `retry_count` 컬럼과 관련 인덱스가 8개 스키마에서 제거되었습니다.

## 멱등성

| 클래스 | 역할 |
|--------|------|
| `ProcessedEvent` | 처리 완료 이벤트 기록 (eventId PK) |
| `IdempotencyFilter` | isDuplicate() / markProcessed() |

모든 이벤트 핸들러와 **Temporal CommandAdapter**에서 중복 처리를 방지합니다. Activity 재시도 시 비즈니스 키 기반 멱등성 키(`UUID.nameUUIDFromBytes("temporal-{operation}:{entityId}")`)로 중복 실행을 차단합니다.

## 분산 락

| 클래스 | 역할 |
|--------|------|
| `@DistributedLock` | AOP 어노테이션 (key, waitTime, leaseTime) |
| `DistributedLockAspect` | Redisson tryLock, SpEL 키 해석 |

사용 예: `@DistributedLock(key = "'lock:inventory:stock:' + #productId")`

## Rate Limiter

| 클래스 | 역할 |
|--------|------|
| `@RateLimiter` | AOP 어노테이션 (key, limit, windowSeconds) |
| `RateLimiterAspect` | Redis Lua Script Sliding Window Counter |
| `RateLimitExceptionHandler` | 429 + Retry-After 헤더 |

사용 예: `@RateLimiter(key = "'order:' + #request.ordererId()", limit = 10, windowSeconds = 60)`

## DLT 모니터링

| 클래스 | 역할 |
|--------|------|
| `DeadLetterEvent` | DLT 이벤트 DB 저장 |
| `DeadLetterConsumer` | `pickme.dead-letter` 토픽 구독 |
| `DeadLetterAdminController` | GET /api/v1/admin/dlt, POST /{eventId}/retry |
| `SlackNotifier` | DLT 적재 + Temporal 워크플로우 실패 시 Slack Webhook 알림 (`sendAlert()` 범용 메서드 포함) |

## 데이터소스 라우팅

| 클래스 | 역할 |
|--------|------|
| `RoutingDataSource` | AbstractRoutingDataSource 기반 읽기/쓰기 분리 |
| `DataSourcePoolConfig` | HikariCP 커넥션 풀 설정 |

## 헬스 체크

| 클래스 | 역할 |
|--------|------|
| `KafkaHealthIndicator` | Kafka 연결 상태 확인 (Spring Actuator) |
| `RedisHealthIndicator` | Redis 가용성 확인 (Spring Actuator) |

## 커스텀 메트릭

`BusinessMetrics` — 주문 TPS, 결제 성공/실패, 재고 차감 지연 시간 (Micrometer → Prometheus)
