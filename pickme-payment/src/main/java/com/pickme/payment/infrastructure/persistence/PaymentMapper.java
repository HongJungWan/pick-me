package com.pickme.payment.infrastructure.persistence;

import com.pickme.payment.domain.model.Payment;
import com.pickme.payment.domain.model.PaymentId;
import com.pickme.payment.domain.model.PaymentMethod;
import com.pickme.payment.domain.model.PaymentStatus;

public final class PaymentMapper {

    private PaymentMapper() {}

    public static PaymentJpaEntity toJpaEntity(Payment p) {
        return new PaymentJpaEntity(
                p.getPaymentId().getValue(), p.getOrderId(), p.getPayerId(), p.getAmount().getAmount(),
                PaymentJpaEntity.PaymentMethodJpa.valueOf(p.getPaymentMethod().name()),
                PaymentJpaEntity.PaymentStatusJpa.valueOf(p.getStatus().name()),
                p.getPgTransactionId(), p.getPaidAt()
        );
    }

    public static Payment toDomain(PaymentJpaEntity e) {
        return Payment.reconstitute(
                PaymentId.of(e.getId()), e.getOrderId(), e.getPayerId(), e.getAmount(),
                PaymentMethod.valueOf(e.getPaymentMethod().name()),
                PaymentStatus.valueOf(e.getStatus().name()),
                e.getPgTransactionId(), e.getPaidAt()
        );
    }
}
