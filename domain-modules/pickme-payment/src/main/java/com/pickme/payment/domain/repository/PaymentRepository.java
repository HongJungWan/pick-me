package com.pickme.payment.domain.repository;

import com.pickme.payment.domain.model.Payment;
import com.pickme.payment.domain.model.PaymentId;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(PaymentId paymentId);

    Optional<Payment> findByOrderId(UUID orderId);
}
