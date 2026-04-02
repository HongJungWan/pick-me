package com.pickme.payment.infrastructure.external;

import com.pickme.payment.domain.model.PgResponse;

import java.util.UUID;

public interface PgPaymentGateway {

    PgResponse requestPayment(UUID paymentId, long amount, String method);

    PgResponse requestRefund(String pgTransactionId, long amount);
}
