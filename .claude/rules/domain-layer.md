---
description: 도메인 계층 코드 작성 시 DDD 순수성 규칙
globs: "**/domain/**/*.java"
---

# 도메인 계층 규칙

## 의존성 금지
- Spring 어노테이션 금지: @Service, @Component, @Repository, @Autowired, @Transactional
- JPA 어노테이션 금지: @Entity, @Table, @Column, @ManyToOne, @Id
- 외부 라이브러리 직접 의존 금지 (Jackson, Lombok @Data 등)
- 허용: @Getter, 순수 Java, 자체 정의 어노테이션

## 설계 원칙
- Primitive 타입 직접 노출 금지 → Value Object로 감싸기
- setter 금지 → 의미 있는 비즈니스 메서드 (예: `order.cancel()`)
- 도메인 이벤트는 Aggregate Root에서만 발행
- Tell, Don't Ask — getter로 꺼내서 외부에서 판단하지 않기

## 이벤트 발행
- 도메인 이벤트는 엔티티 내부에서 등록 (registerEvent)
- 직접 Kafka/MQ 호출 금지 → Outbox 패턴 사용
- 이벤트 클래스는 common 모듈에 정의
