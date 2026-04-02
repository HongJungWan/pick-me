package com.pickme.order.infrastructure.persistence;

import com.pickme.order.domain.model.Address;
import com.pickme.order.domain.model.Money;
import com.pickme.order.domain.model.Order;
import com.pickme.order.domain.model.OrderId;
import com.pickme.order.domain.model.OrderLine;
import com.pickme.order.domain.model.OrderStatus;
import com.pickme.order.domain.model.ShippingInfo;

import java.util.ArrayList;
import java.util.List;

public final class OrderMapper {

    private OrderMapper() {}

    public static OrderJpaEntity toJpaEntity(Order order) {
        List<OrderLineJpaEntity> lines = order.getOrderLines().stream()
                .map(l -> new OrderLineJpaEntity(
                        l.getProductId(), l.getProductName(),
                        l.getQuantity(), l.getUnitPrice().getAmount(), l.getLineTotal().getAmount()))
                .toList();

        return new OrderJpaEntity(
                order.getOrderId().getValue(),
                order.getOrdererId(),
                OrderJpaEntity.OrderStatusJpa.valueOf(order.getStatus().name()),
                order.getTotalAmount().getAmount(),
                order.getShippingInfo().getReceiverName(),
                order.getShippingInfo().getPhone(),
                order.getShippingInfo().getAddress().getZipCode(),
                order.getShippingInfo().getAddress().getRoadAddress(),
                order.getShippingInfo().getAddress().getDetail(),
                order.getOrderedAt(),
                new ArrayList<>(lines)
        );
    }

    public static Order toDomain(OrderJpaEntity entity) {
        List<OrderLine> lines = entity.getOrderLines().stream()
                .map(l -> new OrderLine(l.getProductId(), l.getProductName(),
                        l.getQuantity(), Money.of(l.getUnitPrice())))
                .toList();

        Address address = new Address(entity.getZipCode(), entity.getRoadAddress(), entity.getAddressDetail());
        ShippingInfo shipping = new ShippingInfo(entity.getReceiverName(), entity.getReceiverPhone(), address);

        return Order.reconstitute(
                OrderId.of(entity.getId()),
                entity.getOrdererId(),
                lines,
                OrderStatus.valueOf(entity.getOrderStatus().name()),
                shipping,
                Money.of(entity.getTotalAmount()),
                entity.getOrderedAt()
        );
    }
}
