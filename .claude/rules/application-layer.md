---
description: 어플리케이션 계층 코드 작성 시 서비스, 이벤트 핸들러, CommandAdapter 규칙
globs: "**/application/**/*.java"
---

# 어플리케이션 계층 규칙

## 의존 방향 (ArchUnit IntraDomainLayeringTest 기준)
- Application → Domain (O), Snapshot (O)
- Application → Infrastructure (X, 금지)
- Application → API (X, 금지 — 현재 57건 frozen, Command 패턴 도입 시 해소 예정)

## Service 규칙
- @Service 어노테이션 사용
- 네이밍: `{도메인}Service` (ArchUnit NamingConventionTest 강제)
- @Transactional 경계를 Service에서 관리
- 도메인 모델의 비즈니스 메서드를 호출하고, 이벤트 발행은 DomainEventPublisher에 위임
- 흐름: Request → Service → Aggregate Root 비즈니스 메서드 → DomainEventPublisher → Outbox

## EventHandler 규칙
- @Service 어노테이션 사용
- 네이밍: `{도메인}EventHandler` 또는 `{도메인}SnapshotEventHandler` (ArchUnit 강제)
- 이벤트 수신 후 비즈니스 로직 처리
- 멱등성 필수:
  ```java
  if (idempotencyFilter.isDuplicate(eventId)) return;
  // ... 처리 로직 ...
  idempotencyFilter.markProcessed(eventId, eventType);
  ```
- 스냅샷 갱신: find-or-create → update → save 패턴

## CommandAdapter 규칙 (Temporal 연동)
- 네이밍: `{도메인}CommandAdapter` (ArchUnit NamingConventionTest 강제)
- `com.pickme.orchestration.port.{도메인}CommandPort` 인터페이스 구현 (ArchUnit TemporalIsolationTest 강제)
- 도메인 서비스를 호출하되, Temporal Activity의 멱등성 키를 활용
  - `UUID.nameUUIDFromBytes("temporal-{action}-{domain}:".getBytes())`
- 도메인 모듈이 Temporal SDK에 직접 의존하지 않도록 격리
