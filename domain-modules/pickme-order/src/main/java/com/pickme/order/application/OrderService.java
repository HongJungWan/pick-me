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

        // Temporal 워크플로우 시작 (Shadow/Live 모두 동일한 인터페이스)
        // Shadow 모드: Kafka 코레오그래피가 실제 상태를 관리, 워크플로우는 dry-run 검증
        // Live 모드: 워크플로우가 사가 오케스트레이션 담당
        // Temporal 비활성화 시: NoOpWorkflowStarter가 호출을 무시
        startWorkflow(saved, request);

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
            // 워크플로우 시작 실패가 주문 생성을 방해해서는 안 됨
            log.warn("Temporal 워크플로우 시작 실패 (주문 생성에는 영향 없음): orderId={}", order.getOrderId().getValue(), e);
        }
    }
}
