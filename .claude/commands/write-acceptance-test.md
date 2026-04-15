$ARGUMENTS 시나리오에 대한 인수 테스트를 작성해줘.

## 테스트 구조

- **Given-When-Then** BDD 구조, 각 섹션을 주석으로 구분
- **메서드명**: 한국어 snake_case (예: `주문_생성시_재고가_차감된다`)
- **클래스명**: 영어 (예: `OrderPlacementAcceptanceTest`)

## 기술 스택

- @SpringBootTest + TestContainers (PostgreSQL, Kafka, Redis)
- AssertJ fluent style assertions
- Awaitility로 비동기 이벤트 검증 (Kafka 이벤트 수신 대기)
- 테스트 격리: @Transactional 또는 @DirtiesContext

## 검증 필수 항목

1. **상태 변경**: 핵심 엔티티의 상태가 올바르게 전이되는지
2. **도메인 이벤트**: Outbox 테이블에 이벤트가 기록되는지
3. **부수 효과**: 다른 도메인에 미치는 영향 (이벤트 핸들러 동작)
4. **예외 시나리오**: 실패 케이스에서 보상 트랜잭션이 동작하는지

## 참조 파일

- 해당 도메인의 `domain/service/` 디렉토리 — 비즈니스 로직
- 해당 도메인의 `api/request/`, `api/response/` — API 계약
- `EVENT-CATALOG.md` — 이벤트 정의 및 페이로드
- `common/pickme-common/` — Outbox, 멱등성 키 유틸리티

## 산출물

- 파일 위치: `src/test/java/com/pickme/{domain}/acceptance/`
- 작성 후 실행: `./gradlew :application:pickme-app:test --tests "클래스명"`
- ArchUnit 검증: `./gradlew :independent:pickme-archunit:test`
