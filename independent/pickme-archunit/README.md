# pickme-archunit (아키텍처 테스트)

> ArchUnit 기반 모듈 경계 + 도메인 순수성 CI 강제

## 테스트 규칙

### ModuleBoundaryTest — 모듈 간 직접 import 금지

8개 도메인 모듈이 서로의 패키지를 직접 참조하지 못하도록 강제한다.

```java
// order 모듈은 payment, product, inventory 등을 import할 수 없다
noClasses().that().resideInAPackage("..order..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("..payment..", "..product..", "..inventory..", ...);
```

### DomainPurityTest — 도메인 순수성

domain 패키지에서 Spring, JPA 프레임워크를 import하지 못하도록 강제한다.

```java
// domain → infrastructure, api 참조 금지
// domain → org.springframework 참조 금지
// domain → jakarta.persistence 참조 금지
```

### NamingConventionTest — 네이밍 규칙

- `@RestController` → `*Controller` 접미사
- `@Service` → `*Service` 또는 `*EventHandler` 접미사

## 설정

`archunit.properties`: `archRule.failOnEmptyShould = false` (클래스 미존재 시 허용)
