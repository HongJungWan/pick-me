package com.pickme.order.api.response;

import com.pickme.order.domain.model.Order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        UUID ordererId,
        String status,
        long totalAmount,
        List<OrderLineResponse> orderLines,
        String receiverName,
        Instant orderedAt
) {

    public record OrderLineResponse(UUID productId, String productName, int quantity, long unitPrice, long lineTotal) {}

    public static OrderResponse from(Order order) {
        List<OrderLineResponse> lines = order.getOrderLines().stream()
                .map(l -> new OrderLineResponse(
                        l.getProductId(), l.getProductName(),
                        l.getQuantity(), l.getUnitPrice().getAmount(), l.getLineTotal().getAmount()))
                .toList();

        return new OrderResponse(
                order.getOrderId().getValue(),
                order.getOrdererId(),
                order.getStatus().name(),
                order.getTotalAmount().getAmount(),
                lines,
                order.getShippingInfo().getReceiverName(),
                order.getOrderedAt()
        );
    }
}
