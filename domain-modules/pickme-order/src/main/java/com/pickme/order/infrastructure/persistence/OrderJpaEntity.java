package com.pickme.order.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", schema = "order_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID ordererId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatusJpa orderStatus;

    @Column(nullable = false)
    private long totalAmount;

    @Column(nullable = false, length = 50)
    private String receiverName;

    @Column(nullable = false, length = 20)
    private String receiverPhone;

    @Column(nullable = false, length = 10)
    private String zipCode;

    @Column(nullable = false, length = 200)
    private String roadAddress;

    @Column(length = 200)
    private String addressDetail;

    @Column(nullable = false)
    private Instant orderedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderLineJpaEntity> orderLines = new ArrayList<>();

    public OrderJpaEntity(UUID id, UUID ordererId, OrderStatusJpa orderStatus, long totalAmount,
                          String receiverName, String receiverPhone, String zipCode,
                          String roadAddress, String addressDetail, Instant orderedAt,
                          List<OrderLineJpaEntity> orderLines) {
        this.id = id;
        this.ordererId = ordererId;
        this.orderStatus = orderStatus;
        this.totalAmount = totalAmount;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.zipCode = zipCode;
        this.roadAddress = roadAddress;
        this.addressDetail = addressDetail;
        this.orderedAt = orderedAt;
        this.updatedAt = Instant.now();
        this.orderLines = orderLines;
        orderLines.forEach(l -> l.setOrder(this));
    }

    public void updateStatus(OrderStatusJpa status) {
        this.orderStatus = status;
        this.updatedAt = Instant.now();
    }

    public enum OrderStatusJpa {
        PLACED, PAYMENT_PENDING, PAID, PREPARING, SHIPPED, DELIVERED,
        CANCELLED, REFUND_REQUESTED, REFUNDED
    }
}
