# ArchUnit 베이스라인 부채 백로그

`independent/pickme-archunit/archunit_store/`에 freeze된 67건의 위반을 카테고리별로 분류하고 해소 계획을 정리한 문서. 자세한 규칙↔UUID 매핑은 [`archunit_store/INDEX.md`](../independent/pickme-archunit/archunit_store/INDEX.md) 참고.

## 현황 요약 (2026-04-13 기준, Phase 7-3 적용 후)

| 카테고리 | 건수 | 상태 | 처리 방식 |
|---|---|---|---|
| A. Snapshot read-model 패턴 | 0 | ✅ 해소 완료 | Phase 7-1: layered rule에 Snapshot layer 추가 |
| B. 인프라 포트 누락 (`JwtProvider`/`StockRedisService`) | 0 | ✅ 해소 완료 | Phase 7-3: `TokenIssuer`, `StockCachePort` 인터페이스 도입 |
| G. `LAYERING_NOTIFICATION` "Layer 'API' is empty" | 0 | ✅ 해소 완료 | Phase 7-3: `optionalLayer("API")` 적용 |
| C. `Notification` mutable 필드 | 2 | 🟢 의도적 설계 | freeze 유지 (Aggregate 승격 시 해소) |
| D. 의도적 명명 (`TemporalWorkerBootstrap`) | 1 | 🟢 의도적 설계 | freeze 유지 또는 리네임 |
| E. Application → API Request DTO 의존 | 57 | 🟢 의도적 설계 | freeze 유지 (DDD 해석 차) |
| F. API → Snapshot Entity 의존 | 7 | 🟢 의도적 설계 | freeze 유지 또는 layered rule 추가 조정 |
| **합계** | **67** | | |

## 누적 베이스라인 변화

| 시점 | 베이스라인 | 변화 |
|---|---|---|
| Phase 5 종료 | 137 | (시작) |
| Phase 7-1 (Snapshot layer) | 81 | -56 (-41%) |
| Phase 7-3 (포트화 + API optional) | 67 | -14 (-17%) |
| **누적** | **67** | **-70 (-51%)** |

---

## 카테고리별 상세

### A. Snapshot read-model 패턴 ✅ 해소 완료

**문제**: `application/EventHandler`가 `infrastructure.snapshot.*Entity` / `*SnapshotRepository`를 직접 사용 → application → infrastructure 위반.

**예시 (해소 전)**:
- `OrderSnapshotEventHandler` → `MemberSnapshotEntity`, `ProductSnapshotEntity`
- `SettlementService` → `SalesSnapshotEntity`, `SalesSnapshotRepository`
- `SettlementEventHandler` → `PartnerSnapshotRepository`

**해소 방법**: `IntraDomainLayeringTest`의 layered architecture에 5번째 optional layer "Snapshot" 추가.
```java
.optionalLayer("Snapshot").definedBy(base + ".infrastructure.snapshot..")
.whereLayer("Application").mayOnlyAccessLayers("Domain", "Snapshot")
.whereLayer("Snapshot").mayOnlyAccessLayers("Domain")
```

**효과**: 56건 자동 해소 (134 → 78). order -22, settlement -34.

---

### B. 인프라 포트 누락 ✅ 해소 완료 (Phase 7-3)

**문제 (해소 전)**: 기술 컴포넌트(`JwtProvider`, `StockRedisService`)가 인터페이스 추상화 없이 application 에서 직접 사용됨.

**해소 방법**: `application.port` 패키지에 인터페이스 신설(도메인 순수성 보존 위해 도메인이 아닌 application 에 위치) + 기존 인프라 클래스가 implements:

| 모듈 | 신규 인터페이스 | 어댑터 | application 의존 변경 |
|---|---|---|---|
| member | `member.application.port.TokenIssuer` | `JwtProvider implements TokenIssuer` | `AuthService.jwtProvider` → `tokenIssuer` |
| inventory | `inventory.application.port.StockCachePort` | `StockRedisService implements StockCachePort` | `InventoryCommandAdapter.stockRedisService` / `InventoryEventHandler.stockRedisService` → `stockCache` |

**효과**: inventory 9건 → 0, member 15건 → 11 (잔여는 카테고리 E). 빌드 영향 없음 (전체 컴파일 통과 확인).

---

### C. `Notification` mutable 필드 🟢 의도적 설계

**문제**: `notification.Notification.sendStatus`, `sentAt` 두 필드가 mutable (final 아님). ValueObject 규칙 위반.

**근본 원인**: `Notification`이 `DomainEventProvider`를 구현하지 않아 ValueObject로 분류되지만, 실제로는 발송 상태를 추적하는 mutable 객체. 즉 분류 자체가 부정확.

**해소 후보**:
1. **Aggregate 승격**: `Notification`이 `DomainEventProvider`를 구현하면 ValueObject 규칙에서 자동 제외됨. 발송 완료/실패 시 도메인 이벤트도 발행 가능.
2. **read-only snapshot으로 분리**: `Notification`은 immutable로 두고 `NotificationDeliveryRecord` 같은 별도 객체에 mutable 상태 격리.

**책임자**: 미정 (notification 도메인 오너)

---

### D. 의도적 명명 `TemporalWorkerBootstrap` 🟢 의도적 설계

**문제**: `application.pickme-orchestration.config.TemporalWorkerBootstrap`이 `@Configuration`을 사용하지만 `Bootstrap` 접미사 사용. NamingConvention 규칙 위반.

**해소 후보**:
1. **리네임**: `TemporalWorkerBootstrap` → `TemporalWorkerConfig`
2. **규칙 완화**: `NAMING_CONFIG_SUFFIX`의 허용 접미사에 `Bootstrap` 추가

**판단**: Worker 부트스트랩이라는 의미를 살리는 게 가치 있으면 옵션 2, 일관성을 우선하면 옵션 1.

**책임자**: orchestration 모듈 오너

---

### E. Application → API Request DTO 의존 🟢 의도적 설계

**문제**: application service의 메서드 시그니처가 API 패키지의 request DTO를 직접 받음.

**예시**:
- `OrderService.createOrder(CreateOrderRequest request)` → `order.api.request.CreateOrderRequest`
- `ProductService.createProduct(CreateProductRequest request)` → `product.api.request.CreateProductRequest`

**위반 건수 추정**: order 22 + product 24 ≈ 46건

**DDD 해석 차**:
- 엄격: Controller가 request DTO를 받아 도메인 Command 객체로 변환 후 service 호출
- 실용: service가 직접 request DTO를 받음 (boilerplate 감소)

**해소 후보**:
1. **유지 (현재)**: 실용 패턴으로 freeze
2. **Command 객체 도입**: `CreateOrderCommand` 같은 application 레이어 DTO를 신설하고 controller가 변환 → ~46건 해소 가능

**판단**: 코드 변경 비용이 크고 효과가 추상적이라 현재는 freeze 유지. 차후 Command 패턴 도입 시 일괄 적용.

---

### F. API → Snapshot Entity 의존 🟢 의도적 설계

**문제**: `settlement.api.response.SettlementResponse`의 `from(SalesSnapshotEntity)` 메서드가 snapshot entity를 직접 사용.

**위반 건수**: settlement 7건 (모두 SettlementResponse.from 메서드)

**원인**: API 레이어가 snapshot read-model을 직접 사용하여 응답 생성. CQRS 관점에서 자연스럽지만 layered rule이 API → Snapshot을 허용하지 않음.

**해소 후보**:
1. **layered rule 추가 조정**: API가 Snapshot도 접근 허용 (read-side에 자연스러움)
   ```java
   .whereLayer("API").mayOnlyAccessLayers("Application", "Domain", "Snapshot")
   ```
2. **Application 경유**: SettlementResponse.from을 SettlementService에 두고 API는 Service 응답 사용 → 7건 해소

**판단**: 옵션 1이 코드 변경 없이 즉시 해소 가능. 단, snapshot이 API에 직접 노출되는 게 의도 부합인지 검토 필요.

---

## 갱신 절차

위반 카운트나 카테고리가 변경되면 다음 절차로 본 문서를 동기화:

1. `./gradlew :independent:pickme-archunit:test --rerun-tasks` 실행
2. `archunit_store/INDEX.md`의 카운트 갱신
3. 본 문서의 카테고리별 건수 갱신
4. 신규 위반이 발견되면 새 카테고리 추가
5. 코드 변경 + INDEX.md + 본 문서 + `archunit_store/` 변경분을 함께 커밋

## 우선순위 정책 (잔여 항목)

| 카테고리 | 잔여 | 처리 우선순위 | 근거 |
|---|---|---|---|
| F. API → Snapshot | 7 | MEDIUM | layered rule 1줄 조정 (`API.mayOnlyAccessLayers + "Snapshot"`)으로 해결 가능 |
| E. Application → API DTO | 57 | LOW | DDD 해석 차이, 일괄 Command 객체 도입 시 해소 (큰 비용) |
| D. `Bootstrap` 명명 | 1 | LOW | 1건만 영향, 의미적 정당성 있음 |
| C. `Notification` mutable | 2 | LOW | 도메인 모델 재설계 필요 (`Notification` Aggregate 승격) |
