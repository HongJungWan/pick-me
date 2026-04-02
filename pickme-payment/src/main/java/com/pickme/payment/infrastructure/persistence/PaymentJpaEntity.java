package com.pickme.payment.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", schema = "payment_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentJpaEntity {

    @Id
    private UUID id;
    @Column(nullable = false) private UUID orderId;
    @Column(nullable = false) private UUID payerId;
    @Column(nullable = false) private long amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentMethodJpa paymentMethod;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentStatusJpa status;
    @Column(length = 100) private String pgTransactionId;
    private Instant paidAt;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    public PaymentJpaEntity(UUID id, UUID orderId, UUID payerId, long amount,
                            PaymentMethodJpa paymentMethod, PaymentStatusJpa status,
                            String pgTransactionId, Instant paidAt) {
        this.id = id; this.orderId = orderId; this.payerId = payerId; this.amount = amount;
        this.paymentMethod = paymentMethod; this.status = status;
        this.pgTransactionId = pgTransactionId; this.paidAt = paidAt;
        this.createdAt = Instant.now(); this.updatedAt = Instant.now();
    }

    public void update(PaymentStatusJpa status, String pgTransactionId, Instant paidAt) {
        this.status = status; this.pgTransactionId = pgTransactionId;
        this.paidAt = paidAt; this.updatedAt = Instant.now();
    }

    public enum PaymentMethodJpa { CREDIT_CARD, BANK_TRANSFER, KAKAO_PAY, NAVER_PAY, TOSS_PAY }
    public enum PaymentStatusJpa { REQUESTED, PROCESSING, COMPLETED, FAILED, REFUND_REQUESTED, REFUNDED }
}
