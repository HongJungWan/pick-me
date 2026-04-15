---
description: 테스트 코드 작성 시 BDD 스타일, EDA 검증, MSA 격리 규칙
globs: "**/src/test/**/*.java"
---

# 테스트 작성 규칙

## 구조
- BDD 스타일: `// given`, `// when`, `// then` 주석으로 섹션 구분
- 메서드명: 한국어 snake_case (예: `주문_생성시_재고가_차감된다`)
- 클래스명: 영어 (예: `OrderPlacementAcceptanceTest`)

## Assertion
- AssertJ만 사용 (`assertThat(...).isEqualTo(...)`)
- JUnit assertions (`assertEquals`, `assertTrue`) 금지
- 상태 검증 + 행위 검증 병행

## 통합 테스트 인프라
- @SpringBootTest + TestContainers (PostgreSQL, Kafka, Redis)
- 비동기 이벤트: Awaitility 대기 (`await().atMost(5, SECONDS).until(...)`)
- 테스트 격리: @Transactional 또는 @DirtiesContext

## EDA 검증 필수 항목
- **Outbox 검증**: 비즈니스 로직 실행 후 `outbox_events` 테이블에 이벤트가 기록되는지 확인
- **멱등성 검증**: 동일 eventId로 핸들러를 2회 호출해도 1회만 처리되는지 확인
- **이벤트 페이로드 검증**: aggregateType, aggregateId, eventType, payload 필드 값 확인
- **DLQ 검증**: 처리 실패 시 `dead_letter_events` 테이블에 기록되는지 (실패 시나리오)

## MSA 격리 검증
- 다른 도메인 모듈의 내부 클래스 직접 import 금지
- 스냅샷 테이블을 통한 CQRS Read Model 검증 (직접 JOIN 금지)
- @DistributedLock 동시성 테스트 시 멀티 스레드 시뮬레이션

## 금지 사항
- `Thread.sleep()` 사용 금지 → Awaitility 사용
- 프로덕션 DB 직접 접근 금지 → TestContainers
- KafkaTemplate.send() 직접 호출로 이벤트 발행 금지 → DomainEventPublisher 사용
