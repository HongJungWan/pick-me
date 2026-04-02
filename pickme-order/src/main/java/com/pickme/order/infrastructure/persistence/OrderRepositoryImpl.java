package com.pickme.order.infrastructure.persistence;

import com.pickme.order.domain.model.Order;
import com.pickme.order.domain.model.OrderId;
import com.pickme.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final JpaOrderRepository jpaRepository;

    @Override
    public Order save(Order order) {
        Optional<OrderJpaEntity> existing = jpaRepository.findById(order.getOrderId().getValue());
        if (existing.isPresent()) {
            OrderJpaEntity entity = existing.get();
            entity.updateStatus(OrderJpaEntity.OrderStatusJpa.valueOf(order.getStatus().name()));
            return OrderMapper.toDomain(jpaRepository.save(entity));
        }
        return OrderMapper.toDomain(jpaRepository.save(OrderMapper.toJpaEntity(order)));
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return jpaRepository.findById(orderId.getValue()).map(OrderMapper::toDomain);
    }

    @Override
    public List<Order> findByOrdererId(UUID ordererId) {
        return jpaRepository.findByOrdererId(ordererId).stream().map(OrderMapper::toDomain).toList();
    }
}
