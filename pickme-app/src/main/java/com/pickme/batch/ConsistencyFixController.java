package com.pickme.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/consistency")
@RequiredArgsConstructor
public class ConsistencyFixController {

    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/{orderId}/fix")
    public ResponseEntity<Map<String, Object>> fixOrderConsistency(@PathVariable UUID orderId) {
        try {
            Map<String, Object> order = jdbcTemplate.queryForMap(
                    "SELECT id, order_status FROM order_schema.orders WHERE id = ?", orderId);
            String orderStatus = (String) order.get("order_status");

            Map<String, Object> payment = null;
            try {
                payment = jdbcTemplate.queryForMap(
                        "SELECT id, status FROM payment_schema.payments WHERE order_id = ?", orderId);
            } catch (Exception ignored) {}

            String action;
            if ("PAID".equals(orderStatus) && (payment == null || !"COMPLETED".equals(payment.get("status")))) {
                jdbcTemplate.update(
                        "UPDATE order_schema.orders SET order_status = 'CANCELLED', updated_at = NOW() WHERE id = ?", orderId);
                action = "PAID 주문이지만 결제 없음 → CANCELLED로 보정";
            } else if ("CANCELLED".equals(orderStatus) && payment != null && "COMPLETED".equals(payment.get("status"))) {
                jdbcTemplate.update(
                        "UPDATE payment_schema.payments SET status = 'REFUND_REQUESTED', updated_at = NOW() WHERE order_id = ?", orderId);
                action = "CANCELLED 주문에 COMPLETED 결제 → 환불 요청으로 보정";
            } else if ("PLACED".equals(orderStatus) || "PAYMENT_PENDING".equals(orderStatus)) {
                jdbcTemplate.update(
                        "UPDATE order_schema.orders SET order_status = 'CANCELLED', updated_at = NOW() WHERE id = ?", orderId);
                action = "좀비 주문 → CANCELLED로 보정";
            } else {
                action = "보정 불필요 (정합성 정상)";
            }

            log.info("정합성 수동 보정: orderId={}, action={}", orderId, action);
            return ResponseEntity.ok(Map.of("orderId", orderId, "action", action, "status", "fixed"));
        } catch (Exception e) {
            log.error("정합성 보정 실패: orderId={}", orderId, e);
            return ResponseEntity.internalServerError().body(Map.of("orderId", orderId, "error", e.getMessage()));
        }
    }
}
