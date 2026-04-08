# pickme-archunit (아키텍처 테스트)

> ArchUnit 기반 모듈 경계 + 도메인 순수성 CI 강제

모듈러 모놀리스의 아키텍처 규칙을 컴파일/테스트 시점에 자동으로 검증한다. 모든 빌드(`./gradlew clean build`)에서 실행되므로 규칙 위반 시 CI가 실패한다.

## 테스트 규칙

### ModuleBoundaryTest — 모듈 간 직접 import 금지

8개 도메인 모듈이 서로의 패키지를 직접 참조하지 못하도록 강제한다. 모듈 간 통신은 반드시 **Kafka 도메인 이벤트**를 통해서만 이루어져야 한다.

```java
// order 모듈은 payment, product, inventory 등을 import할 수 없다
noClasses().that().resideInAPackage("..order..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("..payment..", "..product..", "..inventory..", ...);
```

이 규칙이 보장하는 것:
- 모듈 간 **컴파일 타임 의존성 제로** → 향후 MSA 분리 시 코드 변경 최소화
- 공유 모듈(`pickme-common`)만 공통 의존으로 허용

### DomainPurityTest — 도메인 순수성

domain 패키지에서 Spring, JPA 프레임워크를 import하지 못하도록 강제한다. DDD의 핵심인 **도메인 모델의 프레임워크 독립성**을 보장한다.

```java
// domain → infrastructure, api 참조 금지
// domain → org.springframework 참조 금지
// domain → jakarta.persistence 참조 금지
```

이 규칙이 보장하는 것:
- Aggregate Root, Value Object, Domain Event에 `@Entity`, `@Service` 등 프레임워크 어노테이션 누출 0건
- 도메인 로직의 단위 테스트에 Spring Context 불필요

### NamingConventionTest — 네이밍 규칙

| 어노테이션 | 허용 접미사 |
|-----------|-----------|
| `@RestController` | `*Controller` |
| `@Service` | `*Service` 또는 `*EventHandler` |

## 실행

```bash
# 아키텍처 테스트만 실행
./gradlew :independent:pickme-archunit:test

# 전체 빌드에 포함 (기본 동작)
./gradlew clean build
```

## 설정

`archunit.properties`: `archRule.failOnEmptyShould = false` (클래스 미존재 시 허용)
