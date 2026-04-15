---
description: 도메인 계층 코드 작성 시 DDD 순수성 및 전술 패턴 규칙 (ArchUnit DomainPurityTest + DddTacticalPatternTest 기준)
globs: "**/domain/**/*.java"
---

# 도메인 계층 규칙

## 의존성 금지 (ArchUnit DomainPurityTest 강제)
- Spring 어노테이션 금지: @Service, @Component, @Repository, @Autowired, @Transactional
- JPA 어노테이션 금지: @Entity, @Table, @Column, @ManyToOne, @Id
- Lombok 금지: @Data, @Setter, @AllArgsConstructor (ArchUnit DddTacticalPatternTest 강제)
- 허용: @Getter, 순수 Java, 자체 정의 어노테이션

## Aggregate Root 패턴 (이 프로젝트 공통)
- `DomainEventProvider` 인터페이스 구현 필수 (`getDomainEvents()`, `clearDomainEvents()`)
- public 생성자 금지 → private 생성자 + static factory method
  - 생성: `Order.place(...)`, `Partner.register(...)`
  - 복원: `Order.reconstitute(...)`, `Partner.reconstitute(...)`
- 상태 변경은 비즈니스 메서드 내부에서만 (`order.cancel()`, `partner.approve()`)
- 이벤트 등록: 비즈니스 메서드 내부에서 `domainEvents.add(new XxxEvent(...))`
- setter 메서드 완전 금지

## Value Object 패턴
- Primitive 타입 직접 노출 금지 → VO로 감싸기
  - ID: OrderId, PartnerId, MemberId, ProductId, PaymentId, SettlementId (UUID 래퍼)
  - 금액: Money (long amount, 산술 연산 메서드)
  - 복합: Address, ShippingInfo, BusinessInfo, ContractInfo, Email, PhoneNumber
- 모든 인스턴스 필드 final (불변성, ArchUnit DddTacticalPatternTest 강제)
- equals/hashCode → 값 기반 구현
- 생성자에서 유효성 검증 (fail-fast)

## 상태 전이 캡슐화
- Status enum에 전이 규칙을 도메인 내부에 캡슐화
  - 예: `OrderStatus.canTransitionTo(OrderStatus target)`
- 잘못된 전이 시 도메인 예외 발생 (IllegalStateException)

## 이벤트 발행 규칙
- DomainEvent 구현체는 `domain/event/` 패키지에만 위치 (ArchUnit 강제)
- 직접 Kafka/MQ 호출 금지 → Outbox 패턴 사용
- 이벤트 공통 인터페이스: `common/pickme-common/.../event/DomainEvent.java`
- EventEnvelope로 감싸서 버전 정보(v1) 포함

## Repository 포트
- `domain/repository/` 에 인터페이스만 정의
- 구현체는 `infrastructure/persistence/` 에 위치
- Tell, Don't Ask — getter로 꺼내서 외부에서 판단하지 않기
