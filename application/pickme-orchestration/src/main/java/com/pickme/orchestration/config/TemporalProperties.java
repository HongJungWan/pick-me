package com.pickme.orchestration.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "pickme.temporal")
public class TemporalProperties {

    private boolean enabled = false;
    private boolean shadowMode = false;
    private String target = "localhost:7233";
    private String namespace = "default";

    private TaskQueues taskQueues = new TaskQueues();
    private WorkflowConfig workflow = new WorkflowConfig();

    @Getter
    @Setter
    public static class TaskQueues {
        private String orderSaga = "pickme-order-saga";
        private String paymentActivity = "pickme-payment-activity";
        private String inventoryActivity = "pickme-inventory-activity";
        private String settlement = "pickme-settlement";
        private String partner = "pickme-partner";
    }

    @Getter
    @Setter
    public static class WorkflowConfig {
        private OrderFulfillment orderFulfillment = new OrderFulfillment();
        private Refund refund = new Refund();

        @Getter
        @Setter
        public static class OrderFulfillment {
            private String executionTimeout = "30m";
            private String taskTimeout = "30s";
        }

        @Getter
        @Setter
        public static class Refund {
            private String executionTimeout = "1h";
        }
    }
}
