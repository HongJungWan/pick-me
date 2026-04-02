package com.pickme.integration;

import com.pickme.inventory.domain.model.Stock;
import com.pickme.inventory.domain.repository.StockRepository;
import com.pickme.order.application.OrderEventHandler;
import com.pickme.order.application.OrderService;
import com.pickme.order.api.request.CreateOrderRequest;
import com.pickme.order.api.request.CreateOrderRequest.OrderLineRequest;
import com.pickme.order.api.request.CreateOrderRequest.ShippingInfoRequest;
import com.pickme.order.domain.model.Order;
import com.pickme.order.domain.model.OrderId;
import com.pickme.order.domain.model.OrderStatus;
import com.pickme.order.domain.repository.OrderRepository;
import com.pickme.payment.application.PaymentEventHandler;
import com.pickme.payment.domain.model.Payment;
import com.pickme.payment.domain.model.PaymentStatus;
import com.pickme.payment.domain.repository.PaymentRepository;
import com.pickme.inventory.application.InventoryEventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class OrderFlowE2ETest {

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentEventHandler paymentEventHandler;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private InventoryEventHandler inventoryEventHandler;
    @Autowired private StockRepository stockRepository;
    @Autowired private OrderEventHandler orderEventHandler;

    private UUID productId;
    private UUID ordererId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        ordererId = UUID.randomUUID();

        // 재고 50개 생성
        Stock stock = Stock.create(productId, 50);
        stockRepository.save(stock);
    }

    @Test
    void 정상플로우_주문생성_재고예약_결제완료_주문확정() {
        // 1. 주문 생성
        Order order = createOrder(2);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);

        // 2. 재고 예약 (Inventory가 OrderPlacedEvent를 처리)
        UUID reserveEventId = UUID.randomUUID();
        inventoryEventHandler.handleOrderPlaced(reserveEventId, order.getOrderId().getValue(), productId, 2);

        Stock stockAfterReserve = stockRepository.findByProductId(productId).orElseThrow();
        assertThat(stockAfterReserve.getQuantity().getValue()).isEqualTo(48);
        assertThat(stockAfterReserve.getReservedQuantity().getValue()).isEqualTo(2);

        // 3. 결제 처리 (Payment가 OrderPlacedEvent를 처리)
        UUID paymentEventId = UUID.randomUUID();
        paymentEventHandler.handleOrderPlaced(paymentEventId, order.getOrderId().getValue(), ordererId, order.getTotalAmount().getAmount());

        Payment payment = paymentRepository.findByOrderId(order.getOrderId().getValue()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);

        // 4. 주문 확정 (Order가 PaymentCompletedEvent를 처리)
        UUID confirmEventId = UUID.randomUUID();
        orderEventHandler.handlePaymentCompleted(confirmEventId, order.getOrderId().getValue());

        Order confirmedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertThat(confirmedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void 보상플로우_재고부족시_주문실패() {
        // 1. 주문 생성 (수량 100, 재고 50)
        Order order = createOrder(100);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);

        // 2. 재고 예약 시도 → 부족 → InventoryShortageEvent 발행 (reserve에서 이벤트만 발행, 수량 변경 없음)
        UUID reserveEventId = UUID.randomUUID();
        inventoryEventHandler.handleOrderPlaced(reserveEventId, order.getOrderId().getValue(), productId, 100);

        Stock stockAfter = stockRepository.findByProductId(productId).orElseThrow();
        assertThat(stockAfter.getQuantity().getValue()).isEqualTo(50); // 변경 없음
        assertThat(stockAfter.getReservedQuantity().getValue()).isEqualTo(0); // 예약 안 됨

        // 3. Order가 InventoryShortageEvent를 처리 → 주문 취소
        UUID shortageEventId = UUID.randomUUID();
        orderEventHandler.handleInventoryShortage(shortageEventId, order.getOrderId().getValue());

        Order cancelledOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void 보상플로우_결제실패시_주문취소_재고복원() {
        // 1. 주문 생성
        Order order = createOrder(2);

        // 2. 재고 예약
        UUID reserveEventId = UUID.randomUUID();
        inventoryEventHandler.handleOrderPlaced(reserveEventId, order.getOrderId().getValue(), productId, 2);

        Stock stockAfterReserve = stockRepository.findByProductId(productId).orElseThrow();
        assertThat(stockAfterReserve.getQuantity().getValue()).isEqualTo(48);

        // 3. 주문 취소 (결제 실패 시뮬레이션 → Order가 PaymentFailedEvent 처리)
        UUID failEventId = UUID.randomUUID();
        orderEventHandler.handlePaymentFailed(failEventId, order.getOrderId().getValue(), "카드 한도 초과");

        Order cancelledOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // 4. 재고 복원 (Inventory가 OrderCancelledEvent 처리)
        UUID cancelEventId = UUID.randomUUID();
        inventoryEventHandler.handleOrderCancelled(cancelEventId, order.getOrderId().getValue(), productId, 2);

        Stock stockAfterRestore = stockRepository.findByProductId(productId).orElseThrow();
        assertThat(stockAfterRestore.getQuantity().getValue()).isEqualTo(50);
        assertThat(stockAfterRestore.getReservedQuantity().getValue()).isEqualTo(0);
    }

    private Order createOrder(int quantity) {
        CreateOrderRequest request = new CreateOrderRequest(
                ordererId,
                List.of(new OrderLineRequest(productId, "테스트 상품", quantity, 29900)),
                new ShippingInfoRequest("홍정완", "010-1234-5678", "12345", "서울시 강남구", "101호")
        );
        return orderService.createOrder(request);
    }
}
