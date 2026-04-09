package com.pickme.orchestration.activity;

import com.pickme.orchestration.dto.OrderLineItem;
import com.pickme.orchestration.port.InventoryCommandPort;
import com.pickme.orchestration.port.OrderCommandPort;
import com.pickme.orchestration.port.PaymentCommandPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundActivitiesImpl implements RefundActivities {

    private final OrderCommandPort orderCommandPort;
    private final PaymentCommandPort paymentCommandPort;
    private final InventoryCommandPort inventoryCommandPort;

    @Override
    public void requestRefund(UUID orderId, String reason) {
        log.info("[Activity] 환불 요청: orderId={}, reason={}", orderId, reason);
        orderCommandPort.requestRefund(orderId, reason);
    }

    @Override
    public void processRefund(UUID orderId, long refundAmount) {
        log.info("[Activity] PG 환불 처리: orderId={}, amount={}", orderId, refundAmount);
        paymentCommandPort.processRefund(orderId, refundAmount);
    }

    @Override
    public void restoreInventory(UUID orderId, List<OrderLineItem> orderLines) {
        log.info("[Activity] 재고 복원: orderId={}, items={}", orderId, orderLines.size());
        inventoryCommandPort.restoreInventory(orderId, orderLines);
    }

    @Override
    public void completeRefund(UUID orderId) {
        log.info("[Activity] 환불 완료: orderId={}", orderId);
        orderCommandPort.completeRefund(orderId);
    }
}
