package com.pickme.order.application;

import com.pickme.common.event.DomainEventPublisher;
import com.pickme.common.metrics.BusinessMetrics;
import com.pickme.order.api.request.CreateOrderRequest;
import com.pickme.order.domain.model.Address;
import com.pickme.order.domain.model.Money;
import com.pickme.order.domain.model.Order;
import com.pickme.order.domain.model.OrderId;
import com.pickme.order.domain.model.OrderLine;
import com.pickme.order.domain.model.ShippingInfo;
import com.pickme.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;
    private final BusinessMetrics businessMetrics;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        List<OrderLine> orderLines = request.orderLines().stream()
                .map(l -> new OrderLine(l.productId(), l.productName(), l.quantity(), Money.of(l.unitPrice())))
                .toList();

        CreateOrderRequest.ShippingInfoRequest s = request.shippingInfo();
        ShippingInfo shippingInfo = new ShippingInfo(
                s.receiverName(), s.phone(),
                new Address(s.zipCode(), s.roadAddress(), s.addressDetail())
        );

        Order order = Order.place(request.ordererId(), orderLines, shippingInfo);
        Order saved = orderRepository.save(order);
        eventPublisher.publishAll(order);
        businessMetrics.incrementOrderCreated();
        return saved;
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID orderId) {
        return orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByOrderer(UUID ordererId) {
        return orderRepository.findByOrdererId(ordererId);
    }

    @Transactional
    public Order cancelOrder(UUID orderId, String reason) {
        Order order = getOrder(orderId);
        order.cancel(reason);
        Order saved = orderRepository.save(order);
        eventPublisher.publishAll(order);
        businessMetrics.incrementOrderCancelled();
        return saved;
    }
}
