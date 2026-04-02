package com.pickme.notification.application;

import com.pickme.common.idempotency.IdempotencyFilter;
import com.pickme.notification.domain.model.Notification;
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
        notificationRepository.save(Notification.forOrderPlaced(ordererId, orderId));
        idempotencyFilter.markProcessed(eventId, "OrderPlacedEvent");
        log.info("주문 접수 알림 발송: orderId={}", orderId);
    }

    @Transactional
    public void handlePaymentCompleted(UUID eventId, UUID payerId, UUID orderId, long amount) {
        if (idempotencyFilter.isDuplicate(eventId)) return;
        notificationRepository.save(Notification.forPaymentCompleted(payerId, orderId, amount));
        idempotencyFilter.markProcessed(eventId, "PaymentCompletedEvent");
        log.info("결제 완료 알림 발송: orderId={}", orderId);
    }

    @Transactional
    public void handleMemberRegistered(UUID eventId, UUID memberId, String name, String email) {
        if (idempotencyFilter.isDuplicate(eventId)) return;
        notificationRepository.save(Notification.forMemberRegistered(memberId, name));
        idempotencyFilter.markProcessed(eventId, "MemberRegisteredEvent");
        log.info("가입 환영 알림 발송: memberId={}", memberId);
    }

    @Transactional
    public void handleOrderShipped(UUID eventId, UUID ordererId, UUID orderId, String trackingNumber) {
        if (idempotencyFilter.isDuplicate(eventId)) return;
        notificationRepository.save(Notification.forOrderShipped(ordererId, orderId, trackingNumber));
        idempotencyFilter.markProcessed(eventId, "OrderShippedEvent");
        log.info("배송 시작 알림 발송: orderId={}", orderId);
    }

    @Transactional
    public void handleOrderDelivered(UUID eventId, UUID ordererId, UUID orderId) {
        if (idempotencyFilter.isDuplicate(eventId)) return;
        notificationRepository.save(Notification.forOrderDelivered(ordererId, orderId));
        idempotencyFilter.markProcessed(eventId, "OrderDeliveredEvent");
        log.info("배송 완료 알림 발송: orderId={}", orderId);
    }

    @Transactional
    public void handleInventoryShortage(UUID eventId, UUID productId, int requestedQty, int availableQty) {
        if (idempotencyFilter.isDuplicate(eventId)) return;
        notificationRepository.save(Notification.forInventoryShortage(productId, requestedQty, availableQty));
        idempotencyFilter.markProcessed(eventId, "InventoryShortageEvent");
        log.info("재고 부족 운영 알림: productId={}", productId);
    }

    @Transactional
    public void handleSettlementCompleted(UUID eventId, UUID partnerId, long amount) {
        if (idempotencyFilter.isDuplicate(eventId)) return;
        notificationRepository.save(Notification.forSettlementCompleted(partnerId, amount));
        idempotencyFilter.markProcessed(eventId, "SettlementCompletedEvent");
        log.info("정산 완료 알림 발송: partnerId={}", partnerId);
    }
}
