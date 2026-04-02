package com.pickme.common.ratelimit;

public class RateLimitExceededException extends RuntimeException {

    private final int retryAfterSeconds;

    public RateLimitExceededException(String key, int retryAfterSeconds) {
        super("요청 횟수 제한 초과: " + key);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
