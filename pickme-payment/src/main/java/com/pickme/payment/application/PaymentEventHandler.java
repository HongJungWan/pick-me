package com.pickme.payment.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickme.common.event.DomainEvent;
import com.pickme.common.idempotency.IdempotencyFilter;
import com.pickme.common.metrics.BusinessMetrics;
import com.pickme.common.outbox.OutboxEvent;
import com.pickme.common.outbox.OutboxRepository;
import com.pickme.payment.domain.model.Payment;
import com.pickme.payment.domain.model.PaymentMethod;
import com.pickme.payment.domain.model.PgResponse;
import com.pickme.payment.domain.repository.PaymentRepository;
import com.pickme.payment.infrastructure.external.PgPaymentGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final PaymentRepository paymentRepository;
    private final PgPaymentGateway pgPaymentGateway;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final IdempotencyFilter idempotencyFilter;
    private final BusinessMetrics businessMetrics;

    @Transactional
    public void handleOrderPlaced(UUID eventId, UUID orderId, UUID ordererId, long totalAmount) {
        if (idempotencyFilter.isDuplicate(eventId)) {
            log.info("중복 이벤트 무시: eventId={}", eventId);
            return;
        }

        Payment payment = Payment.request(orderId, ordererId, totalAmount, PaymentMethod.CREDIT_CARD);
        payment.process();

        PgResponse pgResponse = pgPaymentGateway.requestPayment(
                payment.getPaymentId().getValue(), totalAmount, PaymentMethod.CREDIT_CARD.name());

        if (pgResponse.isSuccess()) {
            payment.complete(pgResponse);
            businessMetrics.incrementPaymentSuccess();
            log.info("결제 성공: orderId={}, pgTxnId={}", orderId, pgResponse.getTransactionId());
        } else {
            payment.fail(pgResponse.getMessage());
            businessMetrics.incrementPaymentFailed();
            log.warn("결제 실패: orderId={}, reason={}", orderId, pgResponse.getMessage());
        }

        paymentRepository.save(payment);
        publishDomainEvents(payment);
        idempotencyFilter.markProcessed(eventId, "OrderPlacedEvent");
    }

    @Transactional
    public void handleOrderRefundRequested(UUID eventId, UUID orderId, long refundAmount) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("결제를 찾을 수 없습니다: orderId=" + orderId));

        payment.requestRefund();

        PgResponse pgResponse = pgPaymentGateway.requestRefund(payment.getPgTransactionId(), refundAmount);

        if (pgResponse.isSuccess()) {
            payment.refund();
            log.info("환불 성공: orderId={}", orderId);
        } else {
            log.warn("환불 실패: orderId={}, reason={}", orderId, pgResponse.getMessage());
        }

        paymentRepository.save(payment);
        publishDomainEvents(payment);
        idempotencyFilter.markProcessed(eventId, "OrderRefundRequestedEvent");
    }

    private void publishDomainEvents(Payment payment) {
        for (DomainEvent event : payment.getDomainEvents()) {
            try {
                String payload = objectMapper.writeValueAsString(event);
                outboxRepository.save(OutboxEvent.from(event, payload));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("이벤트 직렬화 실패", e);
            }
        }
        payment.clearDomainEvents();
    }
}
