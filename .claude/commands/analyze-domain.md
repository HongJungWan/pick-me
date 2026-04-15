$ARGUMENTS 도메인 모듈의 아키텍처를 분석해줘.

## 분석 항목

1. **패키지 구조**: api / application / domain / infrastructure 4계층 존재 여부
2. **의존 방향 검증**: domain <- application <- api, domain <- infrastructure 규칙 준수 여부
3. **도메인 순수성**: domain 패키지 내 Spring/JPA 어노테이션(@Entity, @Service 등) 사용 여부
4. **Value Object**: Primitive Obsession 확인 — 원시 타입 직접 노출 대신 VO 사용 여부
5. **포트/어댑터**: infrastructure가 domain의 포트(인터페이스)를 구현하는지 확인
6. **이벤트 설계**: Transactional Outbox 패턴 준수, 도메인 이벤트 발행 위치 확인
7. **ArchUnit 위반 가능성**: 현재 ArchUnit 규칙 대비 잠재적 위반 코드 식별

## 참조

- ArchUnit 규칙: independent/pickme-archunit/
- 이벤트 카탈로그: EVENT-CATALOG.md
- 아키텍처 부채: docs/archunit-debt-backlog.md

## 출력 형식

결과를 다음 등급으로 분류해서 보고해줘:
- **Critical**: 아키텍처 규칙 위반 (즉시 수정 필요)
- **Warning**: 잠재적 위반 또는 개선 권장 사항
- **Info**: 참고 정보 또는 잘 되어 있는 부분
