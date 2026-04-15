$ARGUMENTS 시나리오에 대한 인수 테스트를 작성해줘.

## 테스트 구조

- **Given-When-Then** BDD 구조, 각 섹션을 `// given`, `// when`, `// then` 주석으로 구분
- **메서드명**: 한국어 snake_case (예: `주문_생성시_재고가_차감된다`)
- **클래스명**: 영어 (예: `OrderPlacementAcceptanceTest`)

## 기술 스택

- @SpringBootTest + TestContainers (PostgreSQL, Kafka, Redis)
- AssertJ fluent style assertions (JUnit assertEquals/assertTrue 사용 금지)
- Awaitility로 비동기 이벤트 검증 (`await().atMost(5, SECONDS).until(...)`)
- 테스트 격리: @Transactional 또는 @DirtiesContext
- Thread.sleep() 사용 금지 → Awaitility 사용

## 검증 필수 항목 (5계층)

### 1. 상태 전이 검증
- Aggregate Root의 비즈니스 메서드 호출 후 상태가 올바르게 전이되는지
- Status enum의 canTransitionTo() 규칙이 지켜지는지
- 잘못된 전이 시 예외가 발생하는지

### 2. Outbox 이벤트 검증 (EDA)
- 비즈니스 로직 실행 후 `outbox_events` 테이블에 이벤트가 기록되는지
- eventType, aggregateType, aggregateId, payload가 올바른지
- 같은 트랜잭션에서 비즈니스 로직 + Outbox INSERT가 원자적으로 처리되는지

### 3. 멱등성 검증
- 동일 eventId로 이벤트 핸들러를 2회 호출해도 1회만 처리되는지
- `processed_events` 테이블에 기록이 생기는지

### 4. 이벤트 핸들러 부수효과 검증
- Consumer가 이벤트를 수신하면 스냅샷이 갱신되는지
- 보상 트랜잭션이 필요한 실패 케이스에서 롤백 이벤트가 발행되는지

### 5. 분산 락 검증 (해당 시)
- @DistributedLock이 걸린 메서드에 동시 요청 시 순차 처리되는지
- 락 획득 실패 시 적절한 예외가 발생하는지

## 참조 파일

- 해당 도메인의 `domain/model/` — Aggregate Root, VO, Status enum
- 해당 도메인의 `domain/event/` — DomainEvent 구현체
- 해당 도메인의 `application/` — Service, EventHandler
- 해당 도메인의 `infrastructure/messaging/` — Kafka Consumer
- `EVENT-CATALOG.md` — 이벤트 정의 (Publisher/Consumer/Payload)
- `common/pickme-common/src/main/java/com/pickme/common/outbox/` — Outbox 인프라
- `common/pickme-common/src/main/java/com/pickme/common/idempotency/` — 멱등성 인프라
- `common/pickme-common/src/main/java/com/pickme/common/dlt/` — DLQ 인프라

## 산출물

- 파일 위치: `src/test/java/com/pickme/{domain}/acceptance/`
- 작성 후 실행: `./gradlew :application:pickme-app:test --tests "클래스명"`
- ArchUnit 검증: `./gradlew :independent:pickme-archunit:test`
