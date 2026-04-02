package com.pickme.order.contract;

import com.pickme.order.domain.event.OrderPlacedEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Order → Payment 간 이벤트 계약 테스트
 * OrderPlacedEvent의 Payload 구조가 Payment Consumer가 기대하는 형태와 일치하는지 검증
 */
class OrderPaymentContractTest {

    @Test
    void OrderPlacedEvent는_orderId와_totalAmount를_반드시_포함한다() {
        UUID orderId = UUID.randomUUID();
        UUID ordererId = UUID.randomUUID();
        List<OrderPlacedEvent.OrderLinePayload> lines = List.of(
                new OrderPlacedEvent.OrderLinePayload(UUID.randomUUID(), "상품A", 2, 10000, 20000)
        );

        OrderPlacedEvent event = new OrderPlacedEvent(orderId, ordererId, lines, 20000);

        // Payment Consumer가 기대하는 필수 필드
        assertThat(event.getOrderId()).isNotNull();
        assertThat(event.getOrdererId()).isNotNull();
        assertThat(event.getTotalAmount()).isGreaterThan(0);
        assertThat(event.getEventType()).isEqualTo("OrderPlacedEvent");
        assertThat(event.getAggregateType()).isEqualTo("order");
    }

    @Test
    void OrderPlacedEvent는_orderLines를_반드시_포함한다() {
        UUID orderId = UUID.randomUUID();
        UUID ordererId = UUID.randomUUID();
        List<OrderPlacedEvent.OrderLinePayload> lines = List.of(
                new OrderPlacedEvent.OrderLinePayload(UUID.randomUUID(), "상품A", 2, 10000, 20000),
                new OrderPlacedEvent.OrderLinePayload(UUID.randomUUID(), "상품B", 1, 5000, 5000)
        );

        OrderPlacedEvent event = new OrderPlacedEvent(orderId, ordererId, lines, 25000);

        // Inventory Consumer가 기대하는 필수 필드
        assertThat(event.getOrderLines()).hasSize(2);
        assertThat(event.getOrderLines().get(0).productId()).isNotNull();
        assertThat(event.getOrderLines().get(0).quantity()).isGreaterThan(0);
    }
}
