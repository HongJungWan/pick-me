package com.pickme.inventory.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(StringRedisTemplate.class)
public class StockRedisService {

    private final StringRedisTemplate redisTemplate;

    private static final String DEDUCT_SCRIPT = """
            local key = KEYS[1]
            local qty = tonumber(ARGV[1])
            local stock = tonumber(redis.call('GET', key) or '-1')
            if stock == -1 then
                return -1
            end
            if stock < qty then
                return 0
            end
            redis.call('DECRBY', key, qty)
            return 1
            """;

    private static final String RESTORE_SCRIPT = """
            local key = KEYS[1]
            local qty = tonumber(ARGV[1])
            local stock = tonumber(redis.call('GET', key) or '-1')
            if stock == -1 then
                return -1
            end
            redis.call('INCRBY', key, qty)
            return 1
            """;

    private String stockKey(UUID productId) {
        return "stock:" + productId;
    }

    public void initStock(UUID productId, int quantity) {
        redisTemplate.opsForValue().set(stockKey(productId), String.valueOf(quantity));
        log.debug("Redis 재고 초기화: productId={}, qty={}", productId, quantity);
    }

    public int tryDeduct(UUID productId, int quantity) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(DEDUCT_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script,
                Collections.singletonList(stockKey(productId)),
                String.valueOf(quantity));

        if (result == null) return -1;
        return result.intValue();
    }

    public void restore(UUID productId, int quantity) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RESTORE_SCRIPT, Long.class);
        redisTemplate.execute(script,
                Collections.singletonList(stockKey(productId)),
                String.valueOf(quantity));
        log.debug("Redis 재고 복원: productId={}, qty={}", productId, quantity);
    }

    public void syncFromDb(UUID productId, int quantity) {
        redisTemplate.opsForValue().set(stockKey(productId), String.valueOf(quantity));
    }
}
