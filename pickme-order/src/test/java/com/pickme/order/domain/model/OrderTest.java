package com.pickme.order.domain.model;

import com.pickme.order.domain.event.OrderCancelledEvent;
import com.pickme.order.domain.event.OrderConfirmedEvent;
import com.pickme.order.domain.event.OrderPlacedEvent;
import com.pickme.order.domain.event.OrderRefundRequestedEvent;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final UUID ORDERER_ID = UUID.randomUUID();

    @Test
    void 주문생성_정상_OrderPlacedEvent발행() {
        Order order = createOrder();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.getTotalAmount().getAmount()).isEqualTo(59800);
        assertThat(order.getDomainEvents()).hasSize(1);
        assertThat(order.getDomainEvents().get(0)).isInstanceOf(OrderPlacedEvent.class);
    }

    @Test
    void 주문생성_빈항목_예외발생() {
        assertThatThrownBy(() -> Order.place(ORDERER_ID, Collections.emptyList(), createShippingInfo()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1개 이상");
    }

    @Test
    void 상태전이_PLACED에서_PAYMENT_PENDING_성공() {
        Order order = createOrder();
        order.markPaymentPending();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }

    @Test
    void 상태전이_PAYMENT_PENDING에서_PAID_성공() {
        Order order = createOrder();
        order.markPaymentPending();
        order.confirm();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getDomainEvents()).anyMatch(e -> e instanceof OrderConfirmedEvent);
    }

    @Test
    void 상태전이_PAYMENT_PENDING에서_CANCELLED_성공() {
        Order order = createOrder();
        order.markPaymentPending();
        order.cancel("결제 실패");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getDomainEvents()).anyMatch(e -> e instanceof OrderCancelledEvent);
    }

    @Test
    void 상태전이_PLACED에서_CANCELLED_성공() {
        Order order = createOrder();
        order.cancel("고객 요청 취소");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void 상태전이_PAID에서_PREPARING_SHIPPED_DELIVERED() {
        Order order = createOrder();
        order.markPaymentPending();
        order.confirm();
        order.startPreparing();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);
        order.ship();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        order.deliver();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void 상태전이_PAID에서_환불요청_성공() {
        Order order = createOrder();
        order.markPaymentPending();
        order.confirm();
        order.requestRefund("단순 변심");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_REQUESTED);
        assertThat(order.getDomainEvents()).anyMatch(e -> e instanceof OrderRefundRequestedEvent);
    }

    @Test
    void 상태전이_CANCELLED에서_전이불가() {
        Order order = createOrder();
        order.cancel("취소");
        assertThatThrownBy(() -> order.confirm())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("상태 전이 불가");
    }

    @Test
    void 상태전이_DELIVERED에서_전이불가() {
        Order order = createOrder();
        order.markPaymentPending();
        order.confirm();
        order.startPreparing();
        order.ship();
        order.deliver();
        assertThatThrownBy(() -> order.ship())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void Money_음수_예외발생() {
        assertThatThrownBy(() -> Money.of(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void Money_연산() {
        Money a = Money.of(10000);
        Money b = Money.of(5000);
        assertThat(a.add(b).getAmount()).isEqualTo(15000);
        assertThat(a.subtract(b).getAmount()).isEqualTo(5000);
        assertThat(a.multiply(3).getAmount()).isEqualTo(30000);
    }

    @Test
    void OrderLine_수량0_예외발생() {
        assertThatThrownBy(() -> new OrderLine(UUID.randomUUID(), "상품", 0, Money.of(10000)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Order createOrder() {
        List<OrderLine> lines = List.of(
                new OrderLine(UUID.randomUUID(), "테스트 상품", 2, Money.of(29900))
        );
        return Order.place(ORDERER_ID, lines, createShippingInfo());
    }

    private ShippingInfo createShippingInfo() {
        return new ShippingInfo("홍정완", "010-1234-5678",
                new Address("12345", "서울시 강남구 테헤란로 1", "101호"));
    }
}
