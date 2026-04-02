package com.pickme.payment.domain.model;

import com.pickme.payment.domain.event.PaymentCompletedEvent;
import com.pickme.payment.domain.event.PaymentFailedEvent;
import com.pickme.payment.domain.event.RefundCompletedEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID PAYER_ID = UUID.randomUUID();

    @Test
    void 결제요청_정상생성() {
        Payment payment = Payment.request(ORDER_ID, PAYER_ID, 29900, PaymentMethod.CREDIT_CARD);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REQUESTED);
        assertThat(payment.getAmount().getAmount()).isEqualTo(29900);
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
    }

    @Test
    void 상태전이_REQUESTED에서_PROCESSING_성공() {
        Payment payment = createRequestedPayment();
        payment.process();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
    }

    @Test
    void 상태전이_PROCESSING에서_COMPLETED_성공_이벤트발행() {
        Payment payment = createRequestedPayment();
        payment.process();
        payment.complete(PgResponse.success("TXN-12345"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getPgTransactionId()).isEqualTo("TXN-12345");
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(payment.getDomainEvents()).hasSize(1);
        assertThat(payment.getDomainEvents().get(0)).isInstanceOf(PaymentCompletedEvent.class);
    }

    @Test
    void 상태전이_PROCESSING에서_FAILED_성공_이벤트발행() {
        Payment payment = createRequestedPayment();
        payment.process();
        payment.fail("카드 한도 초과");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getDomainEvents()).hasSize(1);
        assertThat(payment.getDomainEvents().get(0)).isInstanceOf(PaymentFailedEvent.class);
    }

    @Test
    void 상태전이_COMPLETED에서_REFUND_REQUESTED에서_REFUNDED() {
        Payment payment = createCompletedPayment();
        payment.requestRefund();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_REQUESTED);

        payment.refund();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getDomainEvents()).anyMatch(e -> e instanceof RefundCompletedEvent);
    }

    @Test
    void 상태전이_FAILED에서_전이불가() {
        Payment payment = createRequestedPayment();
        payment.process();
        payment.fail("실패");

        assertThatThrownBy(() -> payment.complete(PgResponse.success("TXN")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("결제 상태 전이 불가");
    }

    @Test
    void 상태전이_REQUESTED에서_COMPLETED_직접전이_불가() {
        Payment payment = createRequestedPayment();
        assertThatThrownBy(() -> payment.complete(PgResponse.success("TXN")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void Money_음수_예외발생() {
        assertThatThrownBy(() -> Money.of(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Payment createRequestedPayment() {
        return Payment.request(ORDER_ID, PAYER_ID, 29900, PaymentMethod.CREDIT_CARD);
    }

    private Payment createCompletedPayment() {
        Payment payment = createRequestedPayment();
        payment.process();
        payment.complete(PgResponse.success("TXN-12345"));
        payment.clearDomainEvents();
        return payment;
    }
}
