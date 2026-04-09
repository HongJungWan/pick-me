package com.pickme.orchestration.activity;

import com.pickme.orchestration.dto.OrderLineItem;
import com.pickme.orchestration.dto.PaymentResult;
import com.pickme.orchestration.dto.ReserveResult;
import io.temporal.activity.ActivityInterface;

import java.util.List;
import java.util.UUID;

/**
 * 주문 이행 사가에서 사용하는 Activity 정의.
 * 각 메서드는 해당 도메인의 CommandPort를 통해 실제 비즈니스 로직을 호출한다.
 */
@ActivityInterface
public interface OrderActivities {

    ReserveResult reserveInventory(UUID orderId, List<OrderLineItem> items);

    void confirmInventory(UUID orderId, List<OrderLineItem> items);

    void restoreInventory(UUID orderId, List<OrderLineItem> items);

    PaymentResult processPayment(UUID orderId, UUID ordererId, long amount, String paymentMethod);

    void confirmOrder(UUID orderId);

    void cancelOrder(UUID orderId, String reason);
}
