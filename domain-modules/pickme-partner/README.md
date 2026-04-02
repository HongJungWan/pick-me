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

## API

| Method | URI | 설명 |
|--------|-----|------|
| POST | `/api/v1/partners` | 파트너 등록 |
| GET | `/api/v1/partners/{id}` | 파트너 조회 |
| POST | `/api/v1/partners/{id}/approve` | 파트너 승인 |
