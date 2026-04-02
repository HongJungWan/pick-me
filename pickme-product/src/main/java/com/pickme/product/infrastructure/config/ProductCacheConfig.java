package com.pickme.product.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
@EnableCaching
@ConditionalOnBean(RedisConnectionFactory.class)
public class ProductCacheConfig {

    private static final long BASE_TTL_MINUTES = 5;
    private static final long NULL_TTL_MINUTES = 1;

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .entryTtl(withJitter(Duration.ofMinutes(BASE_TTL_MINUTES)))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("product", defaultConfig.entryTtl(withJitter(Duration.ofMinutes(BASE_TTL_MINUTES))));
        cacheConfigurations.put("product-null", defaultConfig.entryTtl(Duration.ofMinutes(NULL_TTL_MINUTES)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    private Duration withJitter(Duration base) {
        long jitter = ThreadLocalRandom.current().nextLong(0, base.toSeconds() / 10 + 1);
        return base.plusSeconds(jitter);
    }
}
