package com.pickme.notification.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Notification {

    private final NotificationId notificationId;
    private final UUID recipientId;
    private final NotificationChannel channel;
    private final String templateCode;
    private final String subject;
    private final String content;
    private SendStatus sendStatus;
    private Instant sentAt;

    private Notification(NotificationId notificationId, UUID recipientId, NotificationChannel channel,
                         String templateCode, String subject, String content, SendStatus sendStatus, Instant sentAt) {
        this.notificationId = notificationId;
        this.recipientId = recipientId;
        this.channel = channel;
        this.templateCode = templateCode;
        this.subject = subject;
        this.content = content;
        this.sendStatus = sendStatus;
        this.sentAt = sentAt;
    }

    public static Notification create(UUID recipientId, NotificationChannel channel,
                                      String templateCode, String subject, String content) {
        return new Notification(
                NotificationId.generate(), recipientId, channel,
                templateCode, subject, content, SendStatus.PENDING, null
        );
    }

    public static Notification forOrderPlaced(UUID ordererId, UUID orderId) {
        Notification n = create(ordererId, NotificationChannel.EMAIL,
                "ORDER_PLACED", "주문 접수 완료",
                String.format("주문이 접수되었습니다. 주문번호: %s", orderId));
        n.send();
        return n;
    }

    public static Notification forPaymentCompleted(UUID payerId, UUID orderId, long amount) {
        Notification n = create(payerId, NotificationChannel.EMAIL,
                "PAYMENT_COMPLETED", "결제 완료",
                String.format("결제가 완료되었습니다. 주문번호: %s, 결제금액: %,d원", orderId, amount));
        n.send();
        return n;
    }

    public static Notification forMemberRegistered(UUID memberId, String name) {
        Notification n = create(memberId, NotificationChannel.EMAIL,
                "MEMBER_WELCOME", "가입을 환영합니다",
                String.format("%s님, pick-me에 가입해주셔서 감사합니다!", name));
        n.send();
        return n;
    }

    public static Notification forOrderShipped(UUID ordererId, UUID orderId, String trackingNumber) {
        Notification n = create(ordererId, NotificationChannel.EMAIL,
                "ORDER_SHIPPED", "배송 시작",
                String.format("상품이 발송되었습니다. 주문번호: %s, 운송장: %s", orderId, trackingNumber));
        n.send();
        return n;
    }

    public static Notification forOrderDelivered(UUID ordererId, UUID orderId) {
        Notification n = create(ordererId, NotificationChannel.EMAIL,
                "ORDER_DELIVERED", "배송 완료",
                String.format("상품이 배송 완료되었습니다. 주문번호: %s", orderId));
        n.send();
        return n;
    }

    public static Notification forInventoryShortage(UUID productId, int requestedQty, int availableQty) {
        Notification n = create(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                NotificationChannel.EMAIL,
                "INVENTORY_SHORTAGE", "재고 부족 알림 (운영)",
                String.format("재고 부족 발생. 상품ID: %s, 요청: %d, 가용: %d", productId, requestedQty, availableQty));
        n.send();
        return n;
    }

    public static Notification forSettlementCompleted(UUID partnerId, long amount) {
        Notification n = create(partnerId, NotificationChannel.EMAIL,
                "SETTLEMENT_COMPLETED", "정산 완료",
                String.format("정산이 완료되었습니다. 정산금액: %,d원", amount));
        n.send();
        return n;
    }

    public static Notification reconstitute(NotificationId id, UUID recipientId, NotificationChannel channel,
                                            String templateCode, String subject, String content,
                                            SendStatus sendStatus, Instant sentAt) {
        return new Notification(id, recipientId, channel, templateCode, subject, content, sendStatus, sentAt);
    }

    public void send() {
        if (this.sendStatus != SendStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 발송할 수 있습니다. 현재: " + this.sendStatus);
        }
        this.sendStatus = SendStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void markFailed() {
        if (this.sendStatus != SendStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 실패 처리할 수 있습니다. 현재: " + this.sendStatus);
        }
        this.sendStatus = SendStatus.FAILED;
    }

    public void markSent() {
        send();
    }

    public NotificationId getNotificationId() { return notificationId; }
    public UUID getRecipientId() { return recipientId; }
    public NotificationChannel getChannel() { return channel; }
    public String getTemplateCode() { return templateCode; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }
    public SendStatus getSendStatus() { return sendStatus; }
    public Instant getSentAt() { return sentAt; }
}
