package com.pickme.payment.infrastructure.persistence;

import com.pickme.payment.domain.model.Payment;
import com.pickme.payment.domain.model.PaymentId;
import com.pickme.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final JpaPaymentRepository jpaRepository;

    @Override
    public Payment save(Payment payment) {
        Optional<PaymentJpaEntity> existing = jpaRepository.findById(payment.getPaymentId().getValue());
        if (existing.isPresent()) {
            PaymentJpaEntity entity = existing.get();
            entity.update(
                    PaymentJpaEntity.PaymentStatusJpa.valueOf(payment.getStatus().name()),
                    payment.getPgTransactionId(), payment.getPaidAt()
            );
            return PaymentMapper.toDomain(jpaRepository.save(entity));
        }
        return PaymentMapper.toDomain(jpaRepository.save(PaymentMapper.toJpaEntity(payment)));
    }

    @Override
    public Optional<Payment> findById(PaymentId paymentId) {
        return jpaRepository.findById(paymentId.getValue()).map(PaymentMapper::toDomain);
    }

    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        return jpaRepository.findByOrderId(orderId).map(PaymentMapper::toDomain);
    }
}
