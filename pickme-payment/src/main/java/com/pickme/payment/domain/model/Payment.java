package com.pickme.payment.domain.model;

import com.pickme.common.event.DomainEvent;
import com.pickme.payment.domain.event.PaymentCompletedEvent;
import com.pickme.payment.domain.event.PaymentFailedEvent;
import com.pickme.payment.domain.event.RefundCompletedEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Payment {

    private final PaymentId paymentId;
    private final UUID orderId;
    private final UUID payerId;
    private final Money amount;
    private final PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String pgTransactionId;
    private Instant paidAt;
    private final List<DomainEvent> domainEvents;

    private Payment(PaymentId paymentId, UUID orderId, UUID payerId, Money amount,
                    PaymentMethod paymentMethod, PaymentStatus status,
                    String pgTransactionId, Instant paidAt) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.payerId = payerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.pgTransactionId = pgTransactionId;
        this.paidAt = paidAt;
        this.domainEvents = new ArrayList<>();
    }

    public static Payment request(UUID orderId, UUID payerId, long amount, PaymentMethod method) {
        return new Payment(
                PaymentId.generate(), orderId, payerId, Money.of(amount),
                method, PaymentStatus.REQUESTED, null, null
        );
    }

    public static Payment reconstitute(PaymentId paymentId, UUID orderId, UUID payerId,
                                       long amount, PaymentMethod method, PaymentStatus status,
                                       String pgTransactionId, Instant paidAt) {
        return new Payment(paymentId, orderId, payerId, Money.of(amount), method, status, pgTransactionId, paidAt);
    }

    public void process() {
        changeStatus(PaymentStatus.PROCESSING);
    }

    public void complete(PgResponse pgResponse) {
        changeStatus(PaymentStatus.COMPLETED);
        this.pgTransactionId = pgResponse.getTransactionId();
        this.paidAt = Instant.now();
        domainEvents.add(new PaymentCompletedEvent(
                paymentId.getValue(), orderId, payerId, amount.getAmount(),
                paymentMethod.name(), pgTransactionId
        ));
    }

    public void fail(String reason) {
        changeStatus(PaymentStatus.FAILED);
        domainEvents.add(new PaymentFailedEvent(paymentId.getValue(), orderId, reason));
    }

    public void requestRefund() {
        changeStatus(PaymentStatus.REFUND_REQUESTED);
    }

    public void refund() {
        changeStatus(PaymentStatus.REFUNDED);
        domainEvents.add(new RefundCompletedEvent(paymentId.getValue(), orderId, amount.getAmount()));
    }

    private void changeStatus(PaymentStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("결제 상태 전이 불가: %s → %s", this.status, newStatus));
        }
        this.status = newStatus;
    }

    public PaymentId getPaymentId() { return paymentId; }
    public UUID getOrderId() { return orderId; }
    public UUID getPayerId() { return payerId; }
    public Money getAmount() { return amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public String getPgTransactionId() { return pgTransactionId; }
    public Instant getPaidAt() { return paidAt; }

    public List<DomainEvent> getDomainEvents() { return Collections.unmodifiableList(domainEvents); }
    public void clearDomainEvents() { domainEvents.clear(); }
}
