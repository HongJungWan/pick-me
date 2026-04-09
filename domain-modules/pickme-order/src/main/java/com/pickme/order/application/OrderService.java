package com.pickme.order.application;

import com.pickme.common.event.DomainEventPublisher;
import com.pickme.common.metrics.BusinessMetrics;
import com.pickme.orchestration.dto.OrderLineItem;
import com.pickme.orchestration.port.WorkflowStarter;
import com.pickme.order.api.request.CreateOrderRequest;
import com.pickme.order.domain.model.Address;
import com.pickme.order.domain.model.Money;
import com.pickme.order.domain.model.Order;
import com.pickme.order.domain.model.OrderId;
import com.pickme.order.domain.model.OrderLine;
import com.pickme.order.domain.model.ShippingInfo;
import com.pickme.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;
    private final BusinessMetrics businessMetrics;
    private final WorkflowStarter workflowStarter;

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

        // TX 커밋 후 워크플로우 시작 — 커밋 전 시작 시 주문 미존재 상태에서 Activity가 실행될 수 있음
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                startWorkflow(saved, request);
            }
        });

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

    private void startWorkflow(Order order, CreateOrderRequest request) {
        try {
            List<OrderLineItem> workflowOrderLines = request.orderLines().stream()
                    .map(l -> new OrderLineItem(l.productId(), l.productName(), l.quantity(), l.unitPrice()))
                    .toList();

            workflowStarter.startOrderFulfillment(
                    order.getOrderId().getValue(),
                    request.ordererId(),
                    workflowOrderLines,
                    order.getTotalAmount().getAmount(),
                    "CREDIT_CARD"
            );
        } catch (Exception e) {
            log.warn("Temporal 워크플로우 시작 실패 (주문 생성에는 영향 없음): orderId={}", order.getOrderId().getValue(), e);
        }
    }
}
