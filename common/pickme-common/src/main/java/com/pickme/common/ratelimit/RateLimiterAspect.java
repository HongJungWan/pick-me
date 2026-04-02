package com.pickme.common.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collections;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnBean(StringRedisTemplate.class)
public class RateLimiterAspect {

    private final StringRedisTemplate redisTemplate;

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private static final String LUA_SCRIPT = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local current = tonumber(redis.call('GET', key) or '0')
            if current >= limit then
                return 0
            end
            current = redis.call('INCR', key)
            if current == 1 then
                redis.call('EXPIRE', key, window)
            end
            return 1
            """;

    @Around("@annotation(rateLimiter)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimiter rateLimiter) throws Throwable {
        String resolvedKey = "ratelimit:" + resolveKey(rateLimiter.key(), joinPoint);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script,
                Collections.singletonList(resolvedKey),
                String.valueOf(rateLimiter.limit()),
                String.valueOf(rateLimiter.windowSeconds()));

        if (result != null && result == 0L) {
            log.warn("Rate limit 초과: key={}, limit={}/{}", resolvedKey, rateLimiter.limit(), rateLimiter.windowSeconds());
            throw new RateLimitExceededException(resolvedKey, rateLimiter.windowSeconds());
        }

        return joinPoint.proceed();
    }

    private String resolveKey(String keyExpression, ProceedingJoinPoint joinPoint) {
        if (!keyExpression.contains("#")) return keyExpression;

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = NAME_DISCOVERER.getParameterNames(method);
        Object[] args = joinPoint.getArgs();

        if (paramNames == null) return keyExpression;

        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        Object value = PARSER.parseExpression(keyExpression).getValue(context);
        return value != null ? value.toString() : keyExpression;
    }
}
