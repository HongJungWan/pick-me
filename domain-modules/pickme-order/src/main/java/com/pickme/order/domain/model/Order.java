package com.pickme.order.domain.model;

import com.pickme.common.event.DomainEvent;
import com.pickme.order.domain.event.OrderCancelledEvent;
import com.pickme.order.domain.event.OrderConfirmedEvent;
import com.pickme.order.domain.event.OrderPlacedEvent;
import com.pickme.order.domain.event.OrderRefundRequestedEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Order implements com.pickme.common.event.DomainEventProvider {

    private final OrderId orderId;
    private final UUID ordererId;
    private final List<OrderLine> orderLines;
    private OrderStatus status;
    private final ShippingInfo shippingInfo;
    private final Money totalAmount;
    private final Instant orderedAt;
    private final List<DomainEvent> domainEvents;

    private Order(OrderId orderId, UUID ordererId, List<OrderLine> orderLines,
                  OrderStatus status, ShippingInfo shippingInfo, Money totalAmount, Instant orderedAt) {
        this.orderId = orderId;
        this.ordererId = ordererId;
        this.orderLines = new ArrayList<>(orderLines);
        this.status = status;
        this.shippingInfo = shippingInfo;
        this.totalAmount = totalAmount;
        this.orderedAt = orderedAt;
        this.domainEvents = new ArrayList<>();
    }

    public static Order place(UUID ordererId, List<OrderLine> orderLines, ShippingInfo shippingInfo) {
        if (orderLines == null || orderLines.isEmpty()) {
            throw new IllegalArgumentException("주문 항목은 1개 이상이어야 합니다");
        }

        Money total = orderLines.stream()
                .map(OrderLine::getLineTotal)
                .reduce(Money.zero(), Money::add);

        Order order = new Order(
                OrderId.generate(), ordererId, orderLines,
                OrderStatus.PLACED, shippingInfo, total, Instant.now()
        );

        List<OrderPlacedEvent.OrderLinePayload> linePayloads = orderLines.stream()
                .map(l -> new OrderPlacedEvent.OrderLinePayload(
                        l.getProductId(), l.getProductName(),
                        l.getQuantity(), l.getUnitPrice().getAmount(), l.getLineTotal().getAmount()))
                .toList();

        order.domainEvents.add(new OrderPlacedEvent(
                order.orderId.getValue(), ordererId, linePayloads, total.getAmount()));

        return order;
    }

    public static Order reconstitute(OrderId orderId, UUID ordererId, List<OrderLine> orderLines,
                                     OrderStatus status, ShippingInfo shippingInfo,
                                     Money totalAmount, Instant orderedAt) {
        return new Order(orderId, ordererId, orderLines, status, shippingInfo, totalAmount, orderedAt);
    }

    public void completePayment() {
        changeStatus(OrderStatus.PAYMENT_PENDING);
        changeStatus(OrderStatus.PAID);
        List<OrderConfirmedEvent.OrderLinePayload> linePayloads = orderLines.stream()
                .map(l -> new OrderConfirmedEvent.OrderLinePayload(l.getProductId(), l.getQuantity()))
                .toList();
        domainEvents.add(new OrderConfirmedEvent(orderId.getValue(), linePayloads));
    }

    public void confirm() {
        changeStatus(OrderStatus.PAID);
        List<OrderConfirmedEvent.OrderLinePayload> linePayloads = orderLines.stream()
                .map(l -> new OrderConfirmedEvent.OrderLinePayload(l.getProductId(), l.getQuantity()))
                .toList();
        domainEvents.add(new OrderConfirmedEvent(orderId.getValue(), linePayloads));
    }

    public void cancel(String reason) {
        changeStatus(OrderStatus.CANCELLED);
        List<OrderCancelledEvent.OrderLinePayload> linePayloads = orderLines.stream()
                .map(l -> new OrderCancelledEvent.OrderLinePayload(l.getProductId(), l.getQuantity()))
                .toList();
        domainEvents.add(new OrderCancelledEvent(orderId.getValue(), reason, linePayloads));
    }

    public void requestRefund(String reason) {
        changeStatus(OrderStatus.REFUND_REQUESTED);
        domainEvents.add(new OrderRefundRequestedEvent(
                orderId.getValue(), totalAmount.getAmount(), reason));
    }

    public void startPreparing() {
        changeStatus(OrderStatus.PREPARING);
    }

    public void ship() {
        changeStatus(OrderStatus.SHIPPED);
    }

    public void deliver() {
        changeStatus(OrderStatus.DELIVERED);
    }

    public void markPaymentPending() {
        changeStatus(OrderStatus.PAYMENT_PENDING);
    }

    private void changeStatus(OrderStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("주문 상태 전이 불가: %s → %s", this.status, newStatus));
        }
        this.status = newStatus;
    }

    public OrderId getOrderId() { return orderId; }
    public UUID getOrdererId() { return ordererId; }
    public List<OrderLine> getOrderLines() { return Collections.unmodifiableList(orderLines); }
    public OrderStatus getStatus() { return status; }
    public ShippingInfo getShippingInfo() { return shippingInfo; }
    public Money getTotalAmount() { return totalAmount; }
    public Instant getOrderedAt() { return orderedAt; }

    public List<DomainEvent> getDomainEvents() { return Collections.unmodifiableList(domainEvents); }
    public void clearDomainEvents() { domainEvents.clear(); }
}
