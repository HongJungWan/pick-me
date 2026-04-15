$ARGUMENTS 도메인 모듈의 아키텍처를 Deep Dive 분석해줘.

## 1단계: 구조 검증

### 패키지 구조 (5-Layer)
`domain-modules/pickme-$ARGUMENTS/src/main/java/com/pickme/$ARGUMENTS/` 아래:
- `api/` — Controller, request/, response/
- `application/` — Service, EventHandler, CommandAdapter
- `domain/model/` — Aggregate Root (DomainEventProvider 구현), Value Object, Enum
- `domain/event/` — DomainEvent 구현체
- `domain/repository/` — Repository 포트 (인터페이스)
- `infrastructure/persistence/` — JPA Entity, RepositoryImpl, Mapper
- `infrastructure/messaging/` — Kafka Consumer
- `infrastructure/snapshot/` — CQRS Read Model 스냅샷 (선택)
- `infrastructure/external/` — ACL Gateway (선택)

누락된 패키지나 잘못된 위치의 클래스를 식별해줘.

### 의존 방향 (ArchUnit IntraDomainLayeringTest 기준)
- API → Application, Domain (O)
- Application → Domain, Snapshot (O)
- Infrastructure → Application, Domain (O)
- Domain → 어디에도 의존하지 않음 (O)
- 위반: Domain → Infrastructure/API/Application (X)

## 2단계: DDD 전술 패턴 검증

### Aggregate Root
- `DomainEventProvider` 구현 여부
- private 생성자 + static factory method (create/place/register + reconstitute)
- 상태 변경 → 비즈니스 메서드 내부에서만 (setter 없음)
- 이벤트 등록 → `domainEvents.add(new XxxEvent(...))` 비즈니스 메서드 내부

### Value Object
- 모든 인스턴스 필드 final (불변성)
- Primitive Obsession 확인 — Long, String, UUID 직접 노출 대신 VO 사용 여부
- equals/hashCode 값 기반 구현
- 생성자 유효성 검증 (fail-fast)

### 상태 전이 규칙
- Status enum에 `canTransitionTo()` 등 전이 규칙이 도메인 내부에 캡슐화되어 있는가

## 3단계: EDA 패턴 검증

### Outbox 패턴
- 도메인 이벤트가 Outbox를 통해 발행되는가 (KafkaTemplate 직접 호출 금지)
- `{module}_schema.outbox_events` 테이블 사용 확인

### 이벤트 카탈로그 정합성
- 모듈에서 발행하는 이벤트가 `EVENT-CATALOG.md`에 등록되어 있는가
- Consumer가 구독하는 이벤트의 Payload 계약이 일치하는가

### 멱등성
- 이벤트 핸들러에서 `IdempotencyFilter` 사용 여부
- 스냅샷 갱신 시 중복 처리 방지 여부

## 참조 파일

- ArchUnit 규칙: `independent/pickme-archunit/src/test/java/com/pickme/archunit/`
- 이벤트 카탈로그: `EVENT-CATALOG.md`
- 아키텍처 부채: `docs/archunit-debt-backlog.md`
- Outbox 구현: `common/pickme-common/src/main/java/com/pickme/common/outbox/`
- 멱등성 구현: `common/pickme-common/src/main/java/com/pickme/common/idempotency/`

## 출력 형식

```
## $ARGUMENTS 도메인 모듈 분석 결과

### 구조: PASS / FAIL
### DDD 전술 패턴: PASS / WARN / FAIL
### EDA 패턴: PASS / WARN / FAIL

### Critical (즉시 수정)
- ...

### Warning (개선 권장)
- ...

### Info (잘 되어 있는 부분)
- ...
```
