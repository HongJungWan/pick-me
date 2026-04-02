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

    @Transactional
    public void handleOrderShipped(UUID eventId, UUID ordererId, UUID orderId, String trackingNumber) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        Notification notification = Notification.create(
                ordererId, NotificationChannel.EMAIL,
                "ORDER_SHIPPED", "배송 시작",
                String.format("상품이 발송되었습니다. 주문번호: %s, 운송장: %s", orderId, trackingNumber)
        );
        notification.markSent();
        notificationRepository.save(notification);

        idempotencyFilter.markProcessed(eventId, "OrderShippedEvent");
        log.info("배송 시작 알림 발송: orderId={}", orderId);
    }

    @Transactional
    public void handleOrderDelivered(UUID eventId, UUID ordererId, UUID orderId) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        Notification notification = Notification.create(
                ordererId, NotificationChannel.EMAIL,
                "ORDER_DELIVERED", "배송 완료",
                String.format("상품이 배송 완료되었습니다. 주문번호: %s", orderId)
        );
        notification.markSent();
        notificationRepository.save(notification);

        idempotencyFilter.markProcessed(eventId, "OrderDeliveredEvent");
        log.info("배송 완료 알림 발송: orderId={}", orderId);
    }

    @Transactional
    public void handleInventoryShortage(UUID eventId, UUID productId, int requestedQty, int availableQty) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        Notification notification = Notification.create(
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                NotificationChannel.EMAIL,
                "INVENTORY_SHORTAGE", "재고 부족 알림 (운영)",
                String.format("재고 부족 발생. 상품ID: %s, 요청: %d, 가용: %d", productId, requestedQty, availableQty)
        );
        notification.markSent();
        notificationRepository.save(notification);

        idempotencyFilter.markProcessed(eventId, "InventoryShortageEvent");
        log.info("재고 부족 운영 알림: productId={}", productId);
    }

    @Transactional
    public void handleSettlementCompleted(UUID eventId, UUID partnerId, long amount) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        Notification notification = Notification.create(
                partnerId, NotificationChannel.EMAIL,
                "SETTLEMENT_COMPLETED", "정산 완료",
                String.format("정산이 완료되었습니다. 정산금액: %,d원", amount)
        );
        notification.markSent();
        notificationRepository.save(notification);

        idempotencyFilter.markProcessed(eventId, "SettlementCompletedEvent");
        log.info("정산 완료 알림 발송: partnerId={}, amount={}", partnerId, amount);
    }
}
