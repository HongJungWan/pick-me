package com.pickme.notification.application;

import com.pickme.common.idempotency.IdempotencyFilter;
import com.pickme.notification.domain.model.Notification;
import com.pickme.notification.domain.model.NotificationChannel;
import com.pickme.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventHandler {

    private final NotificationRepository notificationRepository;
    private final IdempotencyFilter idempotencyFilter;

    @Transactional
    public void handleOrderPlaced(UUID eventId, UUID ordererId, UUID orderId) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        Notification notification = Notification.create(
                ordererId, NotificationChannel.EMAIL,
                "ORDER_PLACED", "주문 접수 완료",
                String.format("주문이 접수되었습니다. 주문번호: %s", orderId)
        );
        notification.markSent();
        notificationRepository.save(notification);

        idempotencyFilter.markProcessed(eventId, "OrderPlacedEvent");
        log.info("주문 접수 알림 발송: orderId={}, recipientId={}", orderId, ordererId);
    }

    @Transactional
    public void handlePaymentCompleted(UUID eventId, UUID payerId, UUID orderId, long amount) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        Notification notification = Notification.create(
                payerId, NotificationChannel.EMAIL,
                "PAYMENT_COMPLETED", "결제 완료",
                String.format("결제가 완료되었습니다. 주문번호: %s, 결제금액: %,d원", orderId, amount)
        );
        notification.markSent();
        notificationRepository.save(notification);

        idempotencyFilter.markProcessed(eventId, "PaymentCompletedEvent");
        log.info("결제 완료 알림 발송: orderId={}, amount={}", orderId, amount);
    }

    @Transactional
    public void handleMemberRegistered(UUID eventId, UUID memberId, String name, String email) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        Notification notification = Notification.create(
                memberId, NotificationChannel.EMAIL,
                "MEMBER_WELCOME", "가입을 환영합니다",
                String.format("%s님, pick-me에 가입해주셔서 감사합니다!", name)
        );
        notification.markSent();
        notificationRepository.save(notification);

        idempotencyFilter.markProcessed(eventId, "MemberRegisteredEvent");
        log.info("가입 환영 알림 발송: memberId={}, email={}", memberId, email);
    }
}
