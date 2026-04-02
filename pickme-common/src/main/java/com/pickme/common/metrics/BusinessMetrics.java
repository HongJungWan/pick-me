package com.pickme.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {

    private final Counter orderCreatedCounter;
    private final Counter orderCancelledCounter;
    private final Counter paymentSuccessCounter;
    private final Counter paymentFailedCounter;
    private final Timer inventoryReserveTimer;

    public BusinessMetrics(MeterRegistry registry) {
        this.orderCreatedCounter = Counter.builder("pickme.order.created.total")
                .description("총 주문 생성 수")
                .register(registry);

        this.orderCancelledCounter = Counter.builder("pickme.order.cancelled.total")
                .description("총 주문 취소 수")
                .register(registry);

        this.paymentSuccessCounter = Counter.builder("pickme.payment.success.total")
                .description("총 결제 성공 수")
                .register(registry);

        this.paymentFailedCounter = Counter.builder("pickme.payment.failed.total")
                .description("총 결제 실패 수")
                .register(registry);

        this.inventoryReserveTimer = Timer.builder("pickme.inventory.reserve.duration")
                .description("재고 차감 소요 시간")
                .register(registry);
    }

    public void incrementOrderCreated() { orderCreatedCounter.increment(); }
    public void incrementOrderCancelled() { orderCancelledCounter.increment(); }
    public void incrementPaymentSuccess() { paymentSuccessCounter.increment(); }
    public void incrementPaymentFailed() { paymentFailedCounter.increment(); }
    public Timer getInventoryReserveTimer() { return inventoryReserveTimer; }
}
