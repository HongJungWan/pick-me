$ARGUMENTS 에 대해 MSA / EDA / DDD 관점의 코드 리뷰를 수행해줘.

변경된 파일을 모두 읽고, 아래 체크리스트를 하나씩 검증한 뒤 결과를 보고해줘.

---

## MSA 준수 사항

### 모듈 격리
- [ ] 도메인 모듈 간 직접 import/메서드 호출이 없는가 (이벤트 또는 orchestration-api CommandPort만 사용)
- [ ] Cross-module JOIN이 없는가 (Schema-per-Module 원칙: order_schema, payment_schema 등 분리)
- [ ] 각 모듈의 독립적인 `outbox_events` / `processed_events` 테이블을 사용하는가

### CQRS Read Model
- [ ] 외부 모듈 데이터가 필요하면 `infrastructure/snapshot/` 의 스냅샷 엔티티를 사용하는가
- [ ] 스냅샷은 이벤트 핸들러에서 갱신되는가 (직접 조회 금지)
- [ ] 스냅샷 갱신 시 IdempotencyFilter로 중복 처리를 방지하는가

### 분산 환경
- [ ] 동시성 제어가 필요한 곳에서 `@DistributedLock` (Redis Redisson)을 사용하는가
- [ ] synchronized/ReentrantLock 등 단일 JVM 락을 사용하지 않는가

---

## EDA 준수 사항

### Transactional Outbox
- [ ] 도메인 이벤트 발행이 비즈니스 로직과 같은 @Transactional 안에서 Outbox INSERT로 이루어지는가
- [ ] KafkaTemplate.send() 등 직접 produce 호출이 없는가 (OutboxRelayScheduler가 릴레이)
- [ ] Outbox 이벤트에 aggregateType, aggregateId, eventType, payload(JSONB)가 모두 포함되는가

### 멱등성
- [ ] 이벤트 컨슈머에서 `idempotencyFilter.isDuplicate(eventId)` 체크가 있는가
- [ ] 처리 완료 후 `idempotencyFilter.markProcessed(eventId, eventType)` 호출이 있는가
- [ ] Temporal Activity에서 멱등성 키 (UUID.nameUUIDFromBytes) 를 사용하는가

### 이벤트 계약
- [ ] 새로운 이벤트가 `EVENT-CATALOG.md`에 등록되어 있는가 (Publisher/Consumer/Payload)
- [ ] DomainEvent 구현체가 `domain/event/` 패키지에 위치하는가
- [ ] EventEnvelope로 감싸져 버전 정보(v1)가 포함되는가

### 실패 처리
- [ ] 컨슈머 예외 시 Kafka DLT(`pickme.dead-letter`)로 전달되는가
- [ ] DeadLetterEvent에 originalTopic, errorMessage가 기록되는가
- [ ] 보상 트랜잭션이 필요한 경우 Temporal Saga 워크플로우로 처리되는가

---

## DDD 준수 사항

### 도메인 순수성
- [ ] `domain/` 패키지에 Spring 어노테이션 없는가 (@Service, @Component, @Repository, @Autowired, @Transactional)
- [ ] `domain/` 패키지에 JPA 어노테이션 없는가 (@Entity, @Table, @Column, @ManyToOne, @Id)
- [ ] `domain/` 패키지에 Lombok @Data, @Setter, @AllArgsConstructor 없는가

### Aggregate Root
- [ ] Aggregate Root가 `DomainEventProvider`를 구현하는가
- [ ] public 생성자 없이 private 생성자 + static factory method (create/reconstitute) 패턴인가
- [ ] 상태 변경이 비즈니스 메서드 내부에서만 이루어지는가 (setter 없음)
- [ ] 도메인 이벤트가 비즈니스 메서드 내부에서 `domainEvents.add(new XxxEvent(...))` 로 등록되는가

### Value Object
- [ ] Primitive 타입 직접 노출 없이 VO로 감싸져 있는가 (Money, OrderId, Email 등)
- [ ] VO의 모든 인스턴스 필드가 final인가 (불변성)
- [ ] equals/hashCode가 값 기반으로 구현되어 있는가
- [ ] 생성자에서 유효성 검증(fail-fast)이 이루어지는가

### 계층 구조
- [ ] 의존 방향: domain ← application ← api, domain ← infrastructure 준수인가
- [ ] Repository 인터페이스가 `domain/repository/`에, 구현체가 `infrastructure/persistence/`에 있는가
- [ ] JPA Entity가 `infrastructure/persistence/` 또는 `infrastructure/snapshot/`에 있는가
- [ ] domain ↔ JPA Entity 변환을 위한 Mapper가 infrastructure에 있는가

### Anti-Corruption Layer
- [ ] 외부 API 호출이 `infrastructure/external/` 의 Gateway 인터페이스를 통하는가
- [ ] 외부 모델 → 내부 도메인 모델 변환이 Gateway 내부에서 이루어지는가

---

## 출력 형식

```
## 코드 리뷰 결과: {대상}

### MSA: PASS / WARN / FAIL
- [x] 통과 항목
- [ ] 위반 항목 → 위반 파일:라인 + 개선 방안

### EDA: PASS / WARN / FAIL
- [x] 통과 항목
- [ ] 위반 항목 → 위반 파일:라인 + 개선 방안

### DDD: PASS / WARN / FAIL
- [x] 통과 항목
- [ ] 위반 항목 → 위반 파일:라인 + 개선 방안

### 종합 판정: APPROVE / REQUEST_CHANGES
- 변경 요청 사항 (있다면)
```
