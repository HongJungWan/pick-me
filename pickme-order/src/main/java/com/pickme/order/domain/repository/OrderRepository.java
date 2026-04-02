package com.pickme.order.domain.repository;

import com.pickme.order.domain.model.Order;
import com.pickme.order.domain.model.OrderId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(OrderId orderId);

    List<Order> findByOrdererId(UUID ordererId);
}
