전체 프로젝트의 아키텍처 규칙 준수 여부를 점검해줘.

## 점검 항목

### 1. ArchUnit 테스트 실행
```bash
./gradlew :independent:pickme-archunit:test --rerun-tasks
```
결과를 분석하고 위반 사항을 보고해줘.

### 2. 모듈 간 의존성 검증
- 8개 도메인 모듈 간 직접 import가 있는지 확인
- 모듈 간 통신이 Kafka 이벤트로만 이루어지는지 검증
- common 모듈의 역방향 의존이 없는지 확인

### 3. 도메인 순수성 검사
각 도메인 모듈의 `domain/` 패키지에서:
- Spring 어노테이션 (@Service, @Component, @Repository, @Autowired) 사용 여부
- JPA 어노테이션 (@Entity, @Table, @Column, @ManyToOne 등) 사용 여부
- 외부 라이브러리 의존 여부

### 4. Outbox 패턴 준수
- 도메인 이벤트가 Outbox 테이블을 통해 발행되는지
- 직접 Kafka produce 호출이 없는지

### 5. 아키텍처 부채 현황
- `docs/archunit-debt-backlog.md` 대비 현재 상태 비교
- freeze된 위반 항목의 현재 수 확인

## 출력 형식

```
## 아키텍처 점검 결과

### ArchUnit 테스트: PASS / FAIL (N violations)
### 모듈 격리: PASS / FAIL (위반 모듈 목록)
### 도메인 순수성: PASS / FAIL (위반 파일 목록)
### Outbox 패턴: PASS / FAIL (직접 produce 위치)
### 부채 현황: 개선 N건 / 악화 N건 / 유지 N건
```
