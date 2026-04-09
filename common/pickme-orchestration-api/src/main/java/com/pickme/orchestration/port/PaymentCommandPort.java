package com.pickme.orchestration.port;

import com.pickme.orchestration.dto.PaymentResult;

import java.util.UUID;

/**
 * 결제 도메인에 대한 명령 포트.
 * Temporal Activity가 이 인터페이스를 통해 결제 처리/환불을 요청한다.
 */
public interface PaymentCommandPort {

    PaymentResult processPayment(UUID orderId, UUID ordererId, long amount, String paymentMethod);

    void processRefund(UUID orderId, long refundAmount);
}
