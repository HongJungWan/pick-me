# pickme-gateway (API Gateway)

> Spring Cloud Gateway — JWT 인증 + 서비스 라우팅

## 라우팅 규칙

| Path | 대상 서비스 | 기본 URL |
|------|-----------|---------|
| `/api/v1/orders/**` | Order Service | http://localhost:8081 |
| `/api/v1/payments/**` | Payment Service | http://localhost:8082 |
| `/api/v1/inventory/**` | Inventory Service | http://localhost:8083 |
| `/api/v1/products/**` | Product Service | http://localhost:8080 |
| `/api/v1/members/**`, `/api/v1/auth/**` | Member Service | http://localhost:8080 |
| `/api/v1/partners/**` | Partner Service | http://localhost:8080 |
| `/api/v1/settlements/**` | Settlement Service | http://localhost:8080 |

서비스 URL은 환경 변수(`ORDER_SERVICE_URL` 등)로 설정 가능.

## JWT 인증 필터 — `JwtAuthenticationFilter`

- `GlobalFilter` 구현 — 모든 요청에 적용
- 공개 경로 (`/api/v1/auth/`, `/api/v1/products`, `/actuator/`) 제외
- 유효한 JWT → `X-Member-Id` 헤더로 memberId를 downstream 서비스에 전파
- 무효한 JWT → 401 Unauthorized 즉시 응답

## MSA 전환 준비

Phase 3에서 모듈을 독립 서비스로 분리하면, 이 Gateway가 단일 진입점 역할을 한다. `docker-compose.msa.yml`에 구성 예시 포함.
