package com.pickme.payment.application;

import com.pickme.common.event.DomainEventPublisher;
import com.pickme.common.idempotency.IdempotencyFilter;
import com.pickme.common.metrics.BusinessMetrics;
import com.pickme.orchestration.dto.PaymentResult;
import com.pickme.orchestration.port.PaymentCommandPort;
import com.pickme.payment.domain.model.Payment;
import com.pickme.payment.domain.model.PaymentMethod;
import com.pickme.payment.domain.model.PaymentStatus;
import com.pickme.payment.domain.repository.PaymentRepository;
import com.pickme.payment.domain.service.PaymentProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * PaymentCommandPort 구현체.
 * Temporal Activity에서 호출되며, 기존 결제 도메인 로직을 재사용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCommandAdapter implements PaymentCommandPort {

    private final PaymentRepository paymentRepository;
    private final PaymentProcessingService paymentProcessingService;
    private final DomainEventPublisher eventPublisher;
    private final IdempotencyFilter idempotencyFilter;
    private final BusinessMetrics businessMetrics;

    @Transactional
    @Override
    public PaymentResult processPayment(UUID orderId, UUID ordererId, long amount, String paymentMethod) {
        UUID idempotencyKey = UUID.nameUUIDFromBytes(
                ("temporal-payment:" + orderId).getBytes(StandardCharsets.UTF_8));

        if (idempotencyFilter.isDuplicate(idempotencyKey)) {
            log.info("중복 Activity 감지 (processPayment): orderId={}", orderId);
            Payment existing = paymentRepository.findByOrderId(orderId).orElse(null);
            if (existing != null) {
                return switch (existing.getStatus()) {
                    case COMPLETED -> PaymentResult.success(existing.getPaymentId().getValue());
                    case REQUESTED, PROCESSING -> PaymentResult.success(existing.getPaymentId().getValue());
                    default -> PaymentResult.failure("결제 실패 상태: " + existing.getStatus());
                };
            }
            return PaymentResult.failure("중복 결제 시도이나 기존 결제를 찾을 수 없음");
        }

        PaymentMethod method = PaymentMethod.valueOf(paymentMethod);
        Payment payment = paymentProcessingService.processNewPayment(orderId, ordererId, amount, method);

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            businessMetrics.incrementPaymentSuccess();
        } else {
            businessMetrics.incrementPaymentFailed();
        }

        paymentRepository.save(payment);
        eventPublisher.publishAll(payment);
        idempotencyFilter.markProcessed(idempotencyKey, "TemporalProcessPayment");

        log.info("[CommandPort] 결제 처리 완료: orderId={}, status={}", orderId, payment.getStatus());

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return PaymentResult.success(payment.getPaymentId().getValue());
        }
        return PaymentResult.failure("결제 실패: " + payment.getStatus());
    }

    @Transactional
    @Override
    public void processRefund(UUID orderId, long refundAmount) {
        UUID idempotencyKey = UUID.nameUUIDFromBytes(
                ("temporal-refund:" + orderId).getBytes(StandardCharsets.UTF_8));

        if (idempotencyFilter.isDuplicate(idempotencyKey)) {
            log.info("중복 Activity 무시 (processRefund): orderId={}", orderId);
            return;
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("결제를 찾을 수 없습니다: orderId=" + orderId));

        paymentProcessingService.processRefund(payment, refundAmount);

        paymentRepository.save(payment);
        eventPublisher.publishAll(payment);
        idempotencyFilter.markProcessed(idempotencyKey, "TemporalProcessRefund");

        log.info("[CommandPort] 환불 처리 완료: orderId={}", orderId);
    }
}
