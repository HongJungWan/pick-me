package com.pickme.inventory.application.port;

import java.util.UUID;

/**
 * 재고 캐시 동기화 포트.
 *
 * <p>구체 구현(Redis, Caffeine 등)은 infrastructure 계층의 어댑터에서 제공한다.
 * application 계층은 본 인터페이스만 의존한다.</p>
 *
 * <p>현재 application 에서 사용되는 메서드만 노출한다 (Interface Segregation).
 * 추가 캐시 연산이 필요하면 본 인터페이스를 확장한다.</p>
 *
 * @see com.pickme.inventory.infrastructure.config.StockRedisService 현 구현체 (Redis 기반)
 */
public interface StockCachePort {

    /** DB 의 최신 재고를 캐시에 동기화한다. */
    void syncFromDb(UUID productId, int quantity);
}
