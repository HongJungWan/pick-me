package com.pickme.orchestration.api;

import com.pickme.orchestration.workflow.OrderFulfillmentWorkflow;
import io.temporal.client.WorkflowClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Temporal 워크플로우 상태 조회 및 관리 REST API.
 * Temporal UI 없이도 프론트엔드/운영 도구에서 워크플로우 상태를 확인할 수 있다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pickme.temporal.enabled", havingValue = "true")
public class WorkflowStatusController {

    private final WorkflowClient workflowClient;

    @GetMapping("/order-fulfillment/{orderId}")
    public ResponseEntity<Map<String, String>> getOrderFulfillmentStatus(@PathVariable UUID orderId) {
        try {
            OrderFulfillmentWorkflow workflow = workflowClient.newWorkflowStub(
                    OrderFulfillmentWorkflow.class, "order-fulfillment-" + orderId);
            String status = workflow.getStatus();
            return ResponseEntity.ok(Map.of(
                    "orderId", orderId.toString(),
                    "workflowId", "order-fulfillment-" + orderId,
                    "status", status
            ));
        } catch (Exception e) {
            log.debug("워크플로우 상태 조회 실패: orderId={}", orderId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/order-fulfillment/{orderId}/cancel")
    public ResponseEntity<Map<String, String>> cancelOrderFulfillment(
            @PathVariable UUID orderId,
            @RequestParam(defaultValue = "관리자 요청") String reason) {
        try {
            OrderFulfillmentWorkflow workflow = workflowClient.newWorkflowStub(
                    OrderFulfillmentWorkflow.class, "order-fulfillment-" + orderId);
            workflow.cancelByAdmin(reason);
            return ResponseEntity.ok(Map.of(
                    "orderId", orderId.toString(),
                    "action", "CANCEL_SIGNAL_SENT",
                    "reason", reason
            ));
        } catch (Exception e) {
            log.warn("워크플로우 취소 시그널 전송 실패: orderId={}", orderId, e);
            return ResponseEntity.notFound().build();
        }
    }
}
