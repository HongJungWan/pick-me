package com.pickme.payment.api.response;

import com.pickme.payment.domain.model.Payment;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        UUID orderId,
        UUID payerId,
        long amount,
        String paymentMethod,
        String status,
        String pgTransactionId,
        Instant paidAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getPaymentId().getValue(), p.getOrderId(), p.getPayerId(),
                p.getAmount().getAmount(), p.getPaymentMethod().name(), p.getStatus().name(),
                p.getPgTransactionId(), p.getPaidAt()
        );
    }
}
