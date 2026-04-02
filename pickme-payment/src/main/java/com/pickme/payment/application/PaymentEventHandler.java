package com.pickme.payment.application;

import com.pickme.common.event.DomainEventPublisher;
import com.pickme.common.idempotency.IdempotencyFilter;
import com.pickme.common.metrics.BusinessMetrics;
import com.pickme.payment.domain.model.Payment;
import com.pickme.payment.domain.model.PaymentMethod;
import com.pickme.payment.domain.model.PaymentStatus;
import com.pickme.payment.domain.repository.PaymentRepository;
import com.pickme.payment.domain.service.PaymentProcessingService;
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
    private final DomainEventPublisher eventPublisher;
    private final IdempotencyFilter idempotencyFilter;
    private final BusinessMetrics businessMetrics;

    @Transactional
    public void handleOrderPlaced(UUID eventId, UUID orderId, UUID ordererId, long totalAmount) {
        if (idempotencyFilter.isDuplicate(eventId)) {
            log.info("중복 이벤트 무시: eventId={}", eventId);
            return;
        }

        PaymentProcessingService processingService = new PaymentProcessingService(pgPaymentGateway);
        Payment payment = processingService.processNewPayment(orderId, ordererId, totalAmount, PaymentMethod.CREDIT_CARD);

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            businessMetrics.incrementPaymentSuccess();
            log.info("결제 성공: orderId={}", orderId);
        } else {
            businessMetrics.incrementPaymentFailed();
            log.warn("결제 실패: orderId={}", orderId);
        }

        paymentRepository.save(payment);
        eventPublisher.publishAll(payment);
        idempotencyFilter.markProcessed(eventId, "OrderPlacedEvent");
    }

    @Transactional
    public void handleOrderRefundRequested(UUID eventId, UUID orderId, long refundAmount) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("결제를 찾을 수 없습니다: orderId=" + orderId));

        PaymentProcessingService processingService = new PaymentProcessingService(pgPaymentGateway);
        processingService.processRefund(payment, refundAmount);

        paymentRepository.save(payment);
        eventPublisher.publishAll(payment);
        idempotencyFilter.markProcessed(eventId, "OrderRefundRequestedEvent");
        log.info("환불 처리 완료: orderId={}", orderId);
    }
}
