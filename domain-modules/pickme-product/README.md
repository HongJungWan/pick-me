# pickme-product (Product Context)

> 상품 등록/수정, Redis 캐시 (Cache Aside + TTL Jitter)

## Aggregate Root — `Product`

| 메서드 | 설명 | 발행 이벤트 |
|--------|------|------------|
| `register()` | 상품 등록 (Factory) | ProductRegisteredEvent |
| `changeName(name)` | 상품명 변경 | ProductInfoChangedEvent |
| `changePrice(price)` | 가격 변경 | ProductPriceChangedEvent |
| `changeDescription(desc)` | 설명 변경 | ProductInfoChangedEvent |
| `changeStatus(status)` | 상태 변경 (canTransitionTo 검증) | - |
| `putOnSale()` / `markSoldOut()` / `hide()` / `discontinue()` | 상태 전이 단축 메서드 | - |

## 상태 전이

```
DRAFT → ON_SALE → SOLD_OUT / HIDDEN → DISCONTINUED
```

## Value Objects

- `ProductId`, `ProductName` (1~200자), `ProductPrice` (할인율 자동 계산), `Category`, `ProductOption`

## Redis 캐시 전략

- **Cache Aside**: `@Cacheable` (조회) / `@CacheEvict` (수정)
- **TTL Jitter**: base 5분 + random 0~10% (동시 만료 방지)
- **Null Object Cache**: 미존재 상품도 1분 TTL 캐시
- **Warm-up**: `ApplicationReadyEvent` 시 Top 100 사전 로딩

## API

| Method | URI | 설명 |
|--------|-----|------|
| POST | `/api/v1/products` | 상품 등록 |
| GET | `/api/v1/products/{id}` | 상품 조회 (캐시) |
| GET | `/api/v1/products` | 상품 목록 |
| PATCH | `/api/v1/products/{id}` | 상품 수정 (캐시 무효화) |
