# pickme-partner (Partner Context)

> 파트너 등록/승인, ACL Gateway 인터페이스

## Aggregate Root — `Partner`

| 메서드 | 설명 | 발행 이벤트 |
|--------|------|------------|
| `register()` | 파트너 등록 (Factory) | - |
| `approve()` | 파트너 승인 (PENDING→APPROVED) | PartnerApprovedEvent |
| `suspend(reason)` | 파트너 정지 (APPROVED→SUSPENDED) | PartnerSuspendedEvent |

## Value Objects

- `PartnerId`, `BusinessInfo` (사업자등록번호, 상호명, 대표자명), `ContractInfo` (수수료율, 정산주기, 계약기간), `PartnerStatus`

## ACL Gateway 인터페이스

외부 API를 내부 도메인 모델로 변환하는 Anti-Corruption Layer:

- `DeliveryGateway` — 택배사 API (requestDelivery, getTrackingInfo)
- `NotificationGateway` — 카카오 알림톡/SMS API (sendKakaoAlimtalk, sendSms)

## 이벤트 흐름

### 발행 이벤트 → Kafka 토픽

| 이벤트 | 토픽 | 소비자 |
|--------|------|--------|
| PartnerApprovedEvent | `pickme.partner.events` | Settlement (파트너 스냅샷 upsert) |
| PartnerSuspendedEvent | `pickme.partner.events` | (향후 상품 비활성화 연동) |

### 구독 이벤트

없음 — Partner는 이벤트 발행만 하는 파트너 관리 서비스.

## API

| Method | URI | 설명 |
|--------|-----|------|
| POST | `/api/v1/partners` | 파트너 등록 |
| GET | `/api/v1/partners/{id}` | 파트너 조회 |
| POST | `/api/v1/partners/{id}/approve` | 파트너 승인 |

## 패키지 구조

```
pickme-partner/
├── api/              PartnerController, Request/Response DTO
├── application/      PartnerService, PartnerCommandAdapter
├── domain/
│   ├── model/        Partner, PartnerId, BusinessInfo, ContractInfo, PartnerStatus
│   ├── event/        PartnerApprovedEvent, PartnerSuspendedEvent
│   └── repository/   PartnerRepository (Interface)
└── infrastructure/
    ├── persistence/  JPA Entity, Mapper, Repository 구현체
    └── external/     DeliveryGateway, NotificationGateway (ACL)
```

## Temporal 연동

`PartnerCommandAdapter`가 `PartnerCommandPort`를 구현하여 `PartnerOnboardingWorkflow` Activity에서 호출된다.

| 메서드 | 설명 | 멱등성 키 |
|--------|------|----------|
| `registerPartner(...)` | 파트너 등록 (PENDING) | `temporal-register-partner:{registrationNumber}` |
| `approvePartner(partnerId)` | 파트너 승인 (APPROVED) | `temporal-approve-partner:{partnerId}` |
| `rejectPartner(partnerId, reason)` | 파트너 거절 (SUSPENDED) | `temporal-reject-partner:{partnerId}` |

`PartnerOnboardingWorkflow`는 `@SignalMethod approveByAdmin/rejectByAdmin`으로 관리자 승인을 대기하며, 최대 7일 타임아웃 후 자동 만료된다.
