# pickme-member (Member Context)

> 회원 가입, JWT 인증, 등급 관리

## Aggregate Root — `Member`

| 메서드 | 설명 | 발행 이벤트 |
|--------|------|------------|
| `register()` | 회원 가입 (Factory) | MemberRegisteredEvent |
| `addPurchaseAmount(amount)` | 구매액 누적 → 등급 재계산 | MemberGradeChangedEvent (등급 변동 시) |
| `changeName(name)` | 이름 변경 | - |
| `changePhone(phone)` | 전화번호 변경 | - |
| `changePassword(password)` | 비밀번호 변경 | - |
| `withdraw()` | 회원 탈퇴 | - |

## 등급 체계

| 등급 | 누적 구매액 기준 |
|------|---------------|
| NORMAL | 0원~ |
| SILVER | 10만원~ |
| GOLD | 50만원~ |
| VIP | 200만원~ |
| VVIP | 1,000만원~ |

## Value Objects

- `MemberId`, `Email` (형식 검증), `Password` (해시), `MemberName` (2~50자), `PhoneNumber` (010-XXXX-XXXX)

## JWT 인증

- `JwtProvider`: Access Token (30분), Refresh Token (7일)
- `SecurityConfig`: Stateless, CSRF 비활성화, 공개 경로 설정
- 로그인 `@RateLimiter`: IP+이메일당 5회/5분

## API

| Method | URI | 설명 |
|--------|-----|------|
| POST | `/api/v1/auth/signup` | 회원 가입 |
| POST | `/api/v1/auth/login` | 로그인 (JWT 발급) |
| GET | `/api/v1/members/{id}` | 회원 조회 |
