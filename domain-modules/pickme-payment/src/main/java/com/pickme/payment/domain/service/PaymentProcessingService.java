package com.pickme.payment.domain.service;

import com.pickme.payment.domain.model.Payment;
import com.pickme.payment.domain.model.PaymentMethod;
import com.pickme.payment.domain.model.PgResponse;

import java.util.UUID;

/**
 * 결제 처리 도메인 서비스
 * PG 연동 + Payment 상태 전이를 단일 비즈니스 행위로 캡슐화
 * Infrastructure 의존성은 Port(인터페이스)를 통해 주입
 */
public class PaymentProcessingService {

    public interface PgGateway {
        PgResponse requestPayment(UUID paymentId, long amount, String method);
        PgResponse requestRefund(String pgTransactionId, long amount);
    }

    private final PgGateway pgGateway;

    public PaymentProcessingService(PgGateway pgGateway) {
        this.pgGateway = pgGateway;
    }

    public Payment processNewPayment(UUID orderId, UUID payerId, long amount, PaymentMethod method) {
        Payment payment = Payment.request(orderId, payerId, amount, method);
        payment.process();

        PgResponse response = pgGateway.requestPayment(
                payment.getPaymentId().getValue(), amount, method.name());

        if (response.isSuccess()) {
            payment.complete(response);
        } else {
            payment.fail(response.getMessage());
        }

        return payment;
    }

    public void processRefund(Payment payment, long refundAmount) {
        payment.requestRefund();

        PgResponse response = pgGateway.requestRefund(
                payment.getPgTransactionId(), refundAmount);

        if (response.isSuccess()) {
            payment.refund();
        }
    }
}
