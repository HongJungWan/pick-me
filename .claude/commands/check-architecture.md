전체 프로젝트의 MSA + EDA + DDD 아키텍처 규칙 준수 여부를 점검해줘.

## 1단계: ArchUnit 테스트 실행 (38개 규칙)

```bash
./gradlew :independent:pickme-archunit:test --rerun-tasks
```

결과를 분석하고, 8개 테스트 클래스별로 PASS/FAIL을 보고해줘:
- DomainPurityTest (4규칙) — 도메인→Spring/JPA/인프라/API 의존 금지
- ModuleBoundaryTest (1규칙) — 8개 모듈 간 직접 의존 금지
- CrossDomainCommunicationTest (2규칙) — 상위↔하위 도메인 의존 금지
- DddTacticalPatternTest (5규칙) — setter/public생성자/VO불변성/이벤트위치/Lombok
- SharedModuleIsolationTest (2규칙) — common/인프라 → 도메인 역방향 의존 금지
- NamingConventionTest (6규칙) — Controller/Service/EventHandler/Config/Gateway/Enum
- IntraDomainLayeringTest (11규칙) — 8개 모듈 레이어링 + Repository 위치
- TemporalIsolationTest (7규칙) — Temporal SDK 격리, 비결정성 API 금지

## 2단계: MSA 격리 검증

### 모듈 간 의존성
- 8개 도메인 모듈 소스에서 다른 도메인 모듈의 패키지를 import하는 코드가 있는지 grep
- `com.pickme.order`에서 `com.pickme.payment` import 등 교차 의존 탐색

### Schema 격리
- Flyway 마이그레이션에서 Cross-schema JOIN이 없는지 확인
- JPA Entity의 @Table(schema=) 설정이 자기 모듈 스키마만 참조하는지

### 직접 Kafka produce 금지
- `KafkaTemplate.send` 호출이 도메인 모듈 내에 없는지 (OutboxRelayScheduler만 허용)

## 3단계: EDA 패턴 검증

### Outbox 정합성
- 각 모듈의 도메인 이벤트가 Outbox를 통해 발행되는지
- DomainEventPublisher → OutboxEvent 저장 흐름 확인

### 이벤트 카탈로그 정합성
- `domain/event/*.java`의 이벤트 클래스와 `EVENT-CATALOG.md` 등록 목록 대조
- 미등록 이벤트 또는 카탈로그에만 있고 구현 없는 이벤트 식별

### 멱등성 구현 확인
- 모든 EventHandler/Consumer에 `IdempotencyFilter` 사용 여부
- `processed_events` 테이블 활용 확인

## 4단계: 아키텍처 부채 현황

`docs/archunit-debt-backlog.md` 대비:
- frozen 위반 수 변화 (현재 67개 기준)
- 카테고리별 증감 (A~F)
- 새로운 위반 카테고리 발생 여부

## 출력 형식

```
## 아키텍처 점검 결과

### ArchUnit (38규칙): PASS / FAIL
- DomainPurityTest: PASS / FAIL (N violations)
- ModuleBoundaryTest: PASS / FAIL
- ...

### MSA 격리: PASS / WARN / FAIL
- 모듈 간 의존: N건 위반
- Schema 격리: PASS / FAIL
- 직접 Kafka produce: N건 발견

### EDA 패턴: PASS / WARN / FAIL
- Outbox 준수: PASS / FAIL
- 이벤트 카탈로그: 미등록 N건 / 미구현 N건
- 멱등성: 미적용 핸들러 N건

### 부채 현황: 총 N건 (이전 67건 대비 +/-N)
- 카테고리별 증감 상세

### 종합 판정: GREEN / YELLOW / RED
```
