package com.pickme.payment.infrastructure.external;

import com.pickme.payment.domain.model.PgResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class MockPgPaymentAdapter implements PgPaymentGateway {

    @Override
    @CircuitBreaker(name = "pg-payment", fallbackMethod = "paymentFallback")
    public PgResponse requestPayment(UUID paymentId, long amount, String method) {
        String transactionId = "PG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Mock PG 결제 승인: paymentId={}, amount={}, txnId={}", paymentId, amount, transactionId);
        return PgResponse.success(transactionId);
    }

    @Override
    @CircuitBreaker(name = "pg-payment", fallbackMethod = "refundFallback")
    public PgResponse requestRefund(String pgTransactionId, long amount) {
        log.info("Mock PG 환불 승인: pgTransactionId={}, amount={}", pgTransactionId, amount);
        return PgResponse.success(pgTransactionId);
    }

    private PgResponse paymentFallback(UUID paymentId, long amount, String method, Throwable t) {
        log.warn("PG 결제 Circuit Breaker 작동: paymentId={}, error={}", paymentId, t.getMessage());
        return PgResponse.failure("PG 서비스 일시 불가: " + t.getMessage());
    }

    private PgResponse refundFallback(String pgTransactionId, long amount, Throwable t) {
        log.warn("PG 환불 Circuit Breaker 작동: txnId={}, error={}", pgTransactionId, t.getMessage());
        return PgResponse.failure("PG 환불 서비스 일시 불가: " + t.getMessage());
    }
}
