# archunit_store 베이스라인 인덱스

`FreezingArchRule`로 freeze된 13개 규칙의 베이스라인 위반 매핑.
`stored.rules`(Java properties 형식, 한국어 escape)를 사람이 읽을 수 있는 형태로 변환한 참고 문서.

## 빠른 참조 (Phase 7-3 포트화 적용 후)

| 규칙 ID | 한국어 이름 | 위반 | 카테고리 |
|---|---|---|---|
| `LAYERING_PRODUCT` | product 도메인 내부 계층 의존성 규칙 | 24 | E. Application → API Request DTO 의존 |
| `LAYERING_ORDER` | order 도메인 내부 계층 의존성 규칙 | 22 | E. Application → API Request DTO 의존 |
| `LAYERING_MEMBER` | member 도메인 내부 계층 의존성 규칙 | 11 | E. Application → API Request DTO 의존 |
| `LAYERING_SETTLEMENT` | settlement 도메인 내부 계층 의존성 규칙 | 7 | F. API → Snapshot Entity (`SettlementResponse.from`) |
| `LAYERING_INVENTORY` | inventory 도메인 내부 계층 의존성 규칙 | 0 | ✅ 해소 (Phase 7-3 `StockCachePort` 도입) |
| `LAYERING_NOTIFICATION` | notification 도메인 내부 계층 의존성 규칙 | 0 | ✅ 해소 (Phase 7-3 `optionalLayer("API")`) |
| `LAYERING_PARTNER` | partner 도메인 내부 계층 의존성 규칙 | 0 | (clean) |
| `LAYERING_PAYMENT` | payment 도메인 내부 계층 의존성 규칙 | 0 | (clean) |
| `DDD_VO_FINAL_FIELDS` | ValueObject 는 불변 (모든 인스턴스 필드가 final) | 2 | C. 발송 상태 추적 필드 (`Notification.sendStatus`, `sentAt`) |
| `DDD_AGGREGATE_PRIVATE_CTOR` | Aggregate Root 는 private 생성자만 허용 | 0 | (clean) |
| `DDD_NO_SETTER` | 도메인 model 패키지의 클래스는 setter 메서드를 가지지 않는다 | 0 | (clean) |
| `DDD_DOMAIN_EVENT_LOCATION` | 도메인 이벤트 클래스는 domain.event 패키지에만 위치한다 | 0 | (clean) |
| `NAMING_CONFIG_SUFFIX` | @Configuration 클래스는 Config / Configuration 접미사를 갖는다 | 1 | D. 의도적 명명 (`TemporalWorkerBootstrap`) |

**합계**: **67줄** 베이스라인 (Phase 6 기준 137 → Phase 7-1 적용 후 81 → Phase 7-3 적용 후 67, **-70건 (-51%)**).

### Phase 7-3 변경 효과

| 영역 | 이전 | 이후 | 처리 |
|---|---|---|---|
| inventory | 9 | 0 | `StockCachePort` 인터페이스 신규 + `StockRedisService implements` |
| notification | 1 | 0 | `optionalLayer("API")` 적용 (이벤트 컨슈머 모듈) |
| member | 15 | 11 | `TokenIssuer` 인터페이스 신규 + `JwtProvider implements` (잔여 11건은 카테고리 E) |

## Phase 7-1 효과 (Snapshot layer 도입)

| 도메인 | 이전 | 이후 | 감소 |
|---|---|---|---|
| order | 44 | 22 | **-22** (snapshot 의존 해소) |
| settlement | 41 | 7 | **-34** (snapshot 의존 해소) |
| product | 24 | 24 | 0 |
| member | 15 | 15 | 0 |
| inventory | 9 | 9 | 0 |
| notification | 1 | 1 | 0 |
| **소계 (layering)** | **134** | **78** | **-56** |

snapshot 패턴이 차지하던 위반은 정상으로 인식 → application → snapshot 의존이 더 이상 위반이 아님.

## 잔여 위반 카테고리 별 해소 계획

### A. 잔여 layering 위반 (53건: order 22, product 24, settlement 7)
snapshot 외의 application → infrastructure 의존. 추가 조사로 무엇인지 식별 후 카테고리 B와 같은 포트화 또는 카테고리 A의 추가 escape hatch 적용.

**다음 단계**: Phase 7-2 가 진행되면 카테고리별 정리 후 본 표 갱신.

### B. 인프라 포트 누락 (~25건)
`JwtProvider` (member 15), `StockRedisService` (inventory 9), 기타 (notification 1) — 기술 컴포넌트가 도메인 인터페이스 정의 없이 application 에서 직접 사용됨.

**해소 계획**: Phase 7-3에서 도메인 포트(`..domain..` 인터페이스) 정의 + 어댑터(impl) infrastructure에 두는 의존성 역전 리팩터링. 약 25건 해소 예상.

### C. 발송 상태 추적 필드 (2건)
`notification.Notification.sendStatus`, `sentAt` — Aggregate가 아닌데 mutable 필드 보유. 알림 발송 결과 추적용.

**해소 후보**:
1. `Notification`을 Aggregate Root로 승격 (`DomainEventProvider` 구현)
2. 또는 read-only snapshot으로 분리하여 mutable 상태를 별도 객체에 격리

현재로서는 의도적 설계로 freeze 유지.

### D. 의도적 명명 (1건)
`TemporalWorkerBootstrap` — `@Configuration`이지만 `Bootstrap` 접미사. Worker 부트스트랩 의미를 살린 명명.

**해소 후보**: `TemporalWorkerConfig`로 리네이밍하거나, NamingConvention 규칙의 허용 접미사에 `Bootstrap`을 추가. 현재 freeze 유지.

## 운영 절차

베이스라인을 갱신할 때는 항상:
1. 코드 수정으로 위반 해소
2. `src/test/resources/archunit.properties`에서 `freeze.refreeze=true` 일시 변경
3. `./gradlew :independent:pickme-archunit:test --rerun-tasks` 1회 실행
4. `freeze.refreeze=false` 복원
5. **본 INDEX.md 의 위반 카운트도 동기화 갱신**
6. `archunit_store/` 변경분 + INDEX.md + 코드 변경분을 함께 커밋

## 파일 구조

```
archunit_store/
├── INDEX.md                                    ← (사람이 읽는 본 문서)
├── stored.rules                                ← (ArchUnit 자동 관리: 규칙 → UUID 매핑)
└── {UUID} × 13                                 ← 각 규칙의 위반 목록 (위반 0건도 빈 파일로 존재)
```

UUID ↔ 영문 ID 매핑은 `stored.rules`의 properties 키 prefix(예: `LAYERING_ORDER`)로 grep하면 즉시 확인 가능. 위반 1건이 새로 들어오면 git diff에서 영문 prefix로 검색해 어느 규칙에 새 위반이 추가됐는지 빠르게 식별 가능하다.
