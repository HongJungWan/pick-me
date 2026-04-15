---
description: 테스트 코드 작성 시 적용되는 BDD 스타일 및 품질 규칙
globs: "**/src/test/**/*.java"
---

# 테스트 작성 규칙

## 구조
- BDD 스타일: `// given`, `// when`, `// then` 주석으로 섹션 구분
- 메서드명: 한국어 snake_case (예: `주문_생성시_재고가_차감된다`)
- 클래스명: 영어 (예: `OrderPlacementTest`)

## Assertion
- AssertJ만 사용 (`assertThat(...).isEqualTo(...)`)
- JUnit assertions (`assertEquals`, `assertTrue`) 금지
- 상태 검증 + 행위 검증 병행

## 통합 테스트
- @SpringBootTest + TestContainers (PostgreSQL, Kafka, Redis)
- 비동기 이벤트: Awaitility로 대기 (`await().atMost(5, SECONDS).until(...)`)
- Outbox 테이블 검증 필수 (도메인 이벤트 발행 확인)

## 금지 사항
- `Thread.sleep()` 사용 금지 → Awaitility 사용
- 프로덕션 DB 직접 접근 금지 → TestContainers
- 다른 도메인 모듈의 내부 클래스 직접 import 금지
