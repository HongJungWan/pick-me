# pickme-archunit (아키텍처 테스트)

> 31개 ArchUnit 규칙으로 모듈 경계 · 도메인 내부 계층 · DDD 전술 패턴을 PR 단계에서 자동 차단

모듈러 모놀리스의 아키텍처 규칙을 컴파일/테스트 시점에 자동 검증한다. 모든 빌드(`./gradlew clean build`)에서 실행되므로 규칙 위반 시 CI가 실패한다.

본 모듈이 막아 주는 것:
- **모듈 경계**: 8개 도메인 모듈 간 직접 의존, 상위→하위 호출, Temporal SDK 누출
- **내부 계층**: `domain → infrastructure` 역참조, application의 JPA Entity 직접 사용, Repository 위치 오류
- **DDD 전술 패턴**: Aggregate Root의 public 생성자, ValueObject의 mutable 필드, setter 메서드, DomainEvent 위치

## 빠른 시작

```bash
# 아키텍처 테스트만 실행 (31개 규칙)
./gradlew :independent:pickme-archunit:test

# 위반 발생 시 상세 출력
./gradlew :independent:pickme-archunit:test --info

# 전체 빌드에 포함 (기본 동작)
./gradlew clean build
```

HTML 리포트: `build/reports/tests/test/index.html`

## 규칙 카탈로그

총 7개 테스트 클래스 · 31개 규칙. 적용 모드는 두 가지로 나뉜다:
- **엄격(strict)**: 위반 0건 보장. 신규 위반 즉시 빌드 실패.
- **freeze**: `FreezingArchRule`로 기존 위반을 베이스라인 처리. 신규 위반만 빌드 실패.

| 테스트 클래스 | 규칙 수 | 적용 모드 | 핵심 검증 |
|---|---|---|---|
| [`ModuleBoundaryTest`](src/test/java/com/pickme/archunit/ModuleBoundaryTest.java) | 1 | 엄격 | 8개 도메인 모듈 상호 격리 (`slices()`) |
| [`DomainPurityTest`](src/test/java/com/pickme/archunit/DomainPurityTest.java) | 4 | 엄격 | `..domain..` 이 Spring/JPA/api/infrastructure 에 비의존 |
| [`NamingConventionTest`](src/test/java/com/pickme/archunit/NamingConventionTest.java) | 3 | 엄격 | Controller / Service / EventHandler 접미사 |
| [`TemporalIsolationTest`](src/test/java/com/pickme/archunit/TemporalIsolationTest.java) | 5 | 엄격 | Temporal SDK 격리 + Activity 도메인 비의존 + CommandAdapter Port 강제 |
| [`IntraDomainLayeringTest`](src/test/java/com/pickme/archunit/IntraDomainLayeringTest.java) | 12 | freeze + 엄격 | 도메인 내부 4계층 + Repository / JPA Entity 위치 |
| [`CrossDomainCommunicationTest`](src/test/java/com/pickme/archunit/CrossDomainCommunicationTest.java) | 2 | 엄격 | 상위 ↔ 하위 도메인 의존 방향 명시 차단 |
| [`DddTacticalPatternTest`](src/test/java/com/pickme/archunit/DddTacticalPatternTest.java) | 4 | freeze | Aggregate Root, ValueObject, Setter, DomainEvent 위치 |
| **합계** | **31** | | |

### ModuleBoundaryTest — 도메인 모듈 상호 격리

8개 도메인 모듈(`order`, `payment`, `product`, `inventory`, `member`, `partner`, `notification`, `settlement`)이 서로의 패키지를 직접 참조하지 못하도록 `slices()` 한 줄로 검증한다. 모듈 목록은 [`DomainModules.NAMES`](src/test/java/com/pickme/archunit/DomainModules.java)에서 가져온다.

```java
slices().matching("com.pickme.(*)..")
    .that(IS_DOMAIN_MODULE)
    .should().notDependOnEachOther();
```

**보장하는 것**: 모듈 간 컴파일 타임 의존성 0건 → 향후 MSA 분리 시 변경 비용 최소화. Cross-domain 통신은 `common:pickme-orchestration-api`의 CommandPort 또는 `common:pickme-common`의 도메인 이벤트로만 가능.

### DomainPurityTest — 도메인 순수성

`..domain..` 패키지가 외부 계층/프레임워크에 의존하지 못하도록 4개 규칙으로 검증.

| 검증 항목 | 차단 대상 |
|---|---|
| 인프라 참조 금지 | `..infrastructure..` |
| API 참조 금지 | `..api..` |
| Spring 어노테이션 금지 | `org.springframework..` |
| JPA 어노테이션 금지 | `jakarta.persistence..`, `javax.persistence..` |

**보장하는 것**: Aggregate Root와 Value Object에 프레임워크 어노테이션 누출 0건. 도메인 로직 단위 테스트에 Spring Context 불필요.

### NamingConventionTest — 네이밍 규칙

| 어노테이션 / 위치 | 허용 접미사 |
|---|---|
| `..api..` 의 `@RestController` | `*Controller` |
| `..application..` 의 `@Service` | `*Service`, `*EventHandler`, `*CommandAdapter` |
| `..application..` 의 클래스명에 `Event` 포함 | `*EventHandler` |

### TemporalIsolationTest — Temporal SDK 격리

Temporal SDK 의존성을 `application:pickme-orchestration` 모듈에 격리하고, Activity가 도메인 모듈을 우회 호출하지 않도록 5개 규칙으로 강제한다.

| 규칙 | 검증 내용 |
|---|---|
| Temporal SDK 격리 | `io.temporal..` 은 `..orchestration..` 모듈에서만 사용 |
| 도메인 → orchestration 비의존 | 도메인 계층은 orchestration 구현체를 알 수 없음 |
| `@ActivityInterface` 위치 | `..orchestration.activity..` 패키지에만 정의 |
| Activity → 도메인 직접 호출 금지 | Activity 구현체는 CommandPort만 거쳐야 함 |
| `*CommandAdapter` ↔ Port 매핑 | CommandAdapter 는 `com.pickme.orchestration.port` 인터페이스를 반드시 구현 |

**보장하는 것**: Temporal 어댑터를 다른 워크플로 엔진(예: Camunda)으로 교체하더라도 도메인/응용 계층 코드는 변경 불필요.

### IntraDomainLayeringTest — 도메인 내부 4계층 + 영속화 위치

각 도메인 모듈 안에서 `api / application / domain / infrastructure` 4계층의 의존 방향을 강제한다. 8개 도메인 × 1개 레이어드 규칙 + Repository/JPA Entity 위치 4개 = 총 12개.

```
API             ──▶ Application, Domain
Application     ──▶ Domain
Infrastructure  ──▶ Application, Domain   (Kafka 컨슈머가 핸들러 호출)
Domain          ──▶ (어떤 계층도 참조 금지)
```

> Infrastructure → Application 은 Kafka 컨슈머(`*EventConsumer`)가 application 패키지의 `*EventHandler`를 호출하는 driven adapter 패턴이라 의도적으로 허용한다.

| 규칙 | 모드 |
|---|---|
| 8개 도메인 레이어드 아키텍처 | freeze (기존 위반 134줄 베이스라인) |
| 도메인 `*Repository` 인터페이스는 `..domain.repository..` 위치 | 엄격 |
| `*RepositoryImpl` 은 `..infrastructure..` 위치 | 엄격 |
| `Jpa*Repository` 는 `..infrastructure..` 위치 | 엄격 |
| `@Entity` 클래스는 `..infrastructure.persistence..` 또는 `..infrastructure.snapshot..` (도메인 모듈만) | 엄격 |

### CrossDomainCommunicationTest — 상위 ↔ 하위 도메인 의존 방향

`ModuleBoundaryTest` 가 모든 도메인 간 의존을 막지만, **상위 → 하위 금지**라는 DDD 의도를 명시적으로 별도 규칙으로 선언한다. 향후 격리 정책이 완화되더라도 방향성은 독립적으로 유지된다.

| 분류 | 도메인 |
|---|---|
| 상위 (orchestration 주체) | `order`, `payment`, `settlement` |
| 하위 (supporting) | `product`, `inventory`, `member`, `partner`, `notification` |

- 상위 → 하위 직접 의존 금지 (정방향)
- 하위 → 상위 직접 의존 금지 (의존성 역전 원칙)

모든 cross-domain 통신은 `common:pickme-orchestration-api` 의 CommandPort 또는 `common:pickme-common` 의 도메인 이벤트로만 한다.

### DddTacticalPatternTest — DDD 전술 패턴

`DomainEventProvider` 마커 인터페이스 구현 여부로 Aggregate Root 와 ValueObject 를 자동 분류하여 패턴을 검증한다. 4개 규칙 모두 freeze 적용.

| 규칙 | 적용 대상 | 베이스라인 위반 |
|---|---|---|
| 도메인 model 클래스의 setter 메서드 금지 | `..domain.model..` 모든 클래스 | 0 |
| Aggregate Root 는 private 생성자만 허용 | `DomainEventProvider` 구현체 | 0 |
| ValueObject 는 모든 인스턴스 필드 final | `..domain.model..` 비-Aggregate, 비-enum, 비-interface | 2 (`notification.Notification.sendStatus`, `sentAt`) |
| DomainEvent 구현체는 `..domain.event..` 에만 위치 | 도메인 모듈의 `DomainEvent` 구현체 | 0 |

## 공용 인프라

### `ArchTestBase` — 공용 분석 범위 메타 어노테이션

기존 4곳에 중복되던 `@AnalyzeClasses` 선언을 메타 어노테이션 하나로 통합. 모든 ArchUnit 테스트 클래스는 `@ArchTestBase` 한 줄만 부착하면 된다.

```java
@ArchTestBase
class MyArchTest { ... }
```

> ArchUnit JUnit5 엔진은 추상 상위 클래스에 선언된 `@AnalyzeClasses` 를 인식하지 못한다 (`@Inherited` 가 있어도). 메타 어노테이션은 인식하므로 상속 대신 메타 어노테이션 방식을 채택했다.

### `DomainModules` — 단일 진실 공급원

도메인 모듈 목록을 한 곳에서 관리. 새 도메인 추가 시 본 클래스만 수정하면 31개 규칙에 자동 반영된다.

```java
static final List<String> NAMES = List.of(
    "order", "payment", "product", "inventory",
    "member", "partner", "notification", "settlement"
);
static final List<String> UPPER = List.of("order", "payment", "settlement");
static final List<String> LOWER = List.of("product", "inventory", "member", "partner", "notification");
```

#### 새 도메인 추가 절차

1. [`DomainModules.NAMES`](src/test/java/com/pickme/archunit/DomainModules.java) 에 도메인 이름 한 줄 추가
2. 비즈니스 성격에 맞춰 `UPPER` 또는 `LOWER` 에 분류 추가
3. [`build.gradle`](build.gradle) 의 `testImplementation` 에 새 도메인 모듈 추가
4. `./gradlew :independent:pickme-archunit:test --rerun-tasks` 실행 — 31개 규칙이 새 도메인에도 자동 적용된다.

## FreezingArchRule 운영 가이드

`IntraDomainLayeringTest` 의 8개 도메인 레이어드 규칙과 `DddTacticalPatternTest` 의 4개 전술 패턴 규칙(총 12개)은 [`FreezingArchRule`](https://www.archunit.org/userguide/html/000_Index.html#_freezing_arch_rules) 로 래핑되어 기존 위반을 베이스라인 처리한다. **신규 위반만 빌드를 실패시킨다.**

### 무엇이 freeze 되어 있나

| 규칙군 | 베이스라인 위반 | 비고 |
|---|---|---|
| `IntraDomainLayering` (8 도메인) | 134줄 | order 44, settlement 41, product 24, member 15, inventory 9, notification 1, partner/payment 0 |
| `DddTacticalPattern` ValueObject 불변성 | 2 | `notification.Notification.sendStatus`, `sentAt` (발송 상태 추적 필드) |
| 그 외 freeze 규칙 (Aggregate 생성자, setter, DomainEvent 위치) | 0 | 신규 위반만 잡힘 |

### `archunit_store/` 는 반드시 git 에 커밋해야 한다

> [!WARNING]
> `archunit_store/` 디렉토리가 git 에 없으면 CI 는 매번 빈 상태에서 베이스라인을 새로 만들고 **현재 시점의 모든 위반을 "허용"으로 등록**한다.
>
> 결과: `IntraDomainLayeringTest` 8개 + `DddTacticalPatternTest` 4개 = **freeze 기반 12개 규칙이 모두 무력화된다.**
>
> ⚠️ **절대 `.gitignore` 에 추가하지 말 것.**

UUID 파일 정체:
- `stored.rules` — 한국어 규칙 이름 ↔ UUID 매핑 (Java properties 형식, 한글은 `\uXXXX` 이스케이프)
- 각 UUID 파일 — 해당 규칙의 베이스라인 위반 목록 (사람이 직접 편집하지 않음)

### 위반 해소 시 store 갱신 절차

1. 코드 수정으로 위반을 해소
2. [`archunit.properties`](src/test/resources/archunit.properties) 에서 `freeze.refreeze=false` → `true` 로 변경
3. `./gradlew :independent:pickme-archunit:test --rerun-tasks` 1회 실행 → store 자동 갱신
4. `freeze.refreeze=true` → `false` 로 복원
5. `archunit_store/` 변경분과 코드 변경분을 함께 커밋

### 신규 위반이 발생한 경우

- 빌드 실패 메시지에 신규 위반의 클래스/필드/생성자 위치가 출력된다.
- **기본 처리**: 위반 코드를 수정하여 규칙에 맞게 변경 (권장).
- **예외 처리** (의도된 escape hatch): 동료 리뷰 + PR 본문에 정당화 사유 명시 후, 위 갱신 절차로 store 에 추가. 베이스라인 증가는 기술 부채로 간주.

## 새 ArchUnit 규칙 추가 가이드

1. 새 테스트 파일에 `@ArchTestBase` 부착 (필수)
2. 도메인 모듈을 다루는 규칙은 `DomainModules` 헬퍼 사용 — 패키지 이름 하드코딩 금지
3. 기존 위반이 있을 가능성이 있으면 `FreezingArchRule.freeze(...)` 로 래핑 → 첫 실행에서 store 자동 생성
4. 위반 0건이 확실하면 freeze 없이 엄격 적용 (가장 강한 보장)

## Negative test (검증 패턴)

새 규칙 도입 시 일부러 위반을 만들어 빌드가 실패하는지 확인 후 원복하는 패턴. Phase 1~4 작업에서 검증 완료된 시나리오:

| 검증 시나리오 | 차단되는 규칙 |
|---|---|
| `order.application` 에서 `payment.domain` 클래스 import | `ModuleBoundaryTest`, `CrossDomainCommunicationTest` |
| `partner.application` 에서 `partner.infrastructure.persistence` 클래스 직접 사용 | `IntraDomainLayeringTest` (Application → Infrastructure 신규 위반) |
| `partner.domain.model` 에 mutable 필드 + setter 가진 클래스 추가 | `DddTacticalPatternTest` (VO 불변성, setter 금지) |

probe 클래스 명명 규칙은 `_*Probe.java` (밑줄 prefix). 작업 후 반드시 제거.

## 설정

`src/test/resources/archunit.properties`:

| 속성 | 값 | 의미 |
|---|---|---|
| `archRule.failOnEmptyShould` | `false` | 매칭되는 클래스가 0건일 때 규칙 통과 (도메인 미생성 단계 허용) |
| `freeze.store.default.path` | `archunit_store` | 베이스라인 저장 위치 (모듈 루트 기준 상대 경로) |
| `freeze.store.default.allowStoreCreation` | `true` | 첫 실행 시 store 자동 생성 |
| `freeze.refreeze` | `false` | 위반 감소 시 자동 갱신 비활성화 (위 운영 절차로 수동 갱신) |
