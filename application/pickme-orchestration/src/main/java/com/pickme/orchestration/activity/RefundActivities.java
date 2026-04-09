package com.pickme.orchestration.activity;

import com.pickme.orchestration.dto.OrderLineItem;
import io.temporal.activity.ActivityInterface;

import java.util.List;
import java.util.UUID;

@ActivityInterface
public interface RefundActivities {

    void requestRefund(UUID orderId, String reason);

    void processRefund(UUID orderId, long refundAmount);

    void restoreInventory(UUID orderId, List<OrderLineItem> orderLines);

    void completeRefund(UUID orderId);
}
