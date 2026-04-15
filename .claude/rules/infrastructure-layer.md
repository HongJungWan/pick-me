---
description: 인프라스트럭처 계층 코드 작성 시 MSA 격리, Outbox, 멱등성, ACL 규칙
globs: "**/infrastructure/**/*.java"
---

# 인프라스트럭처 계층 규칙

## 패키지 구조 (ArchUnit IntraDomainLayeringTest + NamingConventionTest 기준)
- `persistence/` — JPA Entity (@Entity), RepositoryImpl, JpaRepository, Mapper
- `messaging/` — Kafka Consumer (@KafkaListener), 이벤트 역직렬화
- `snapshot/` — CQRS Read Model 스냅샷 엔티티 (다른 모듈 데이터의 비정규화 사본)
- `external/` — ACL Gateway (외부 API → 내부 도메인 모델 변환)

## JPA Entity 규칙
- @Entity 클래스는 `infrastructure/persistence/` 또는 `infrastructure/snapshot/` 에만 위치
- 도메인 모델(domain/model/)과 JPA Entity는 분리 — Mapper로 변환
- @Table(schema = "{module}_schema") — 자기 모듈 스키마만 참조 (Cross-schema JOIN 금지)

## Repository 구현 규칙
- `domain/repository/`의 포트 인터페이스를 구현
- Spring Data JPA 인터페이스(`JpaXxxRepository`)는 infrastructure 내부에서만 사용
- 도메인 모델 ↔ JPA Entity 변환은 Mapper 클래스에서 수행

## Kafka Consumer 규칙 (EDA)
- @KafkaListener로 토픽 구독
- JSON 역직렬화 후 IdempotencyFilter로 중복 체크 필수
  - `if (idempotencyFilter.isDuplicate(eventId)) return;`
  - 처리 후 `idempotencyFilter.markProcessed(eventId, eventType);`
- Manual Acknowledgment: `ack.acknowledge()`
- 예외 발생 시 RuntimeException throw → Kafka DLT로 전달

## Outbox 연동 규칙
- 도메인 이벤트 발행은 DomainEventPublisher → OutboxEvent 저장으로만 수행
- KafkaTemplate.send() 직접 호출 금지 (OutboxRelayScheduler만 Kafka produce)
- OutboxEvent 필수 필드: eventId(UUID), aggregateType, aggregateId, eventType, payload(JSONB)

## CQRS 스냅샷 규칙 (infrastructure/snapshot/)
- 다른 모듈의 데이터가 필요하면 스냅샷 엔티티로 비정규화
- 스냅샷은 이벤트 핸들러(application/)에서만 갱신 — 직접 조회/동기 호출 금지
- 예: order 모듈의 ProductSnapshotEntity, MemberSnapshotEntity

## ACL Gateway 규칙 (infrastructure/external/)
- 외부 API 모델 → 내부 도메인 모델 변환을 Gateway 내부에서 수행
- 도메인 계층이 외부 API 모델에 직접 의존하지 않도록 격리
- Gateway 구현체는 infrastructure/external/ 에 위치 (ArchUnit NamingConventionTest 강제)
- record 타입 반환값 사용 (DeliveryResult, SendResult 등)
