package com.pickme.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsistencyCheckBatch {

    private static final Duration ZOMBIE_THRESHOLD = Duration.ofHours(2);
    private static final Duration ORPHAN_THRESHOLD = Duration.ofMinutes(5);

    private final JdbcTemplate jdbcTemplate;

    @Value("${pickme.temporal.enabled:false}")
    private boolean temporalEnabled;

    @Scheduled(cron = "0 0 3 * * *")
    public void checkConsistency() {
        log.info("=== 정합성 검증 배치 시작 ===");

        if (temporalEnabled) {
            checkOrphanOrders();
        } else {
            checkZombieOrders();
        }

        // 주문-결제 정합성 검증은 Temporal 활성화와 무관하게 안전망으로 유지
        checkOrderPaymentConsistency();

        log.info("=== 정합성 검증 배치 완료 ===");
    }

    /**
     * Temporal 모드: 주문은 생성되었으나 워크플로우가 시작되지 않은 고아 주문 탐지.
     * afterCommit() 콜백 실패 시 발생 가능.
     */
    private void checkOrphanOrders() {
        Instant threshold = Instant.now().minus(ORPHAN_THRESHOLD);

        try {
            List<Map<String, Object>> orphans = jdbcTemplate.queryForList(
                    "SELECT id, orderer_id, order_status, ordered_at FROM order_schema.orders " +
                    "WHERE order_status = 'PLACED' AND ordered_at < ?",
                    java.sql.Timestamp.from(threshold)
            );

            if (!orphans.isEmpty()) {
                log.warn("고아 주문 후보 감지 (Temporal 모드): {}건 (PLACED 상태 {}분 이상 유지)",
                        orphans.size(), ORPHAN_THRESHOLD.toMinutes());
                orphans.forEach(o -> log.warn("  - orderId={}, orderedAt={} — 워크플로우 존재 여부 확인 필요",
                        o.get("id"), o.get("ordered_at")));
                // TODO: Temporal API로 workflow 존재 여부 확인 후 미존재 시 워크플로우 재시작
            } else {
                log.info("고아 주문 없음");
            }
        } catch (Exception e) {
            log.warn("고아 주문 검증 스킵 (테이블 미존재 가능): {}", e.getMessage());
        }
    }

    private void checkZombieOrders() {
        Instant threshold = Instant.now().minus(ZOMBIE_THRESHOLD);

        try {
            List<Map<String, Object>> zombies = jdbcTemplate.queryForList(
                    "SELECT id, orderer_id, order_status, ordered_at FROM order_schema.orders " +
                    "WHERE order_status IN ('PLACED', 'PAYMENT_PENDING') AND ordered_at < ?",
                    java.sql.Timestamp.from(threshold)
            );

            if (!zombies.isEmpty()) {
                log.warn("좀비 주문 감지: {}건 (PLACED/PAYMENT_PENDING 상태 {}시간 이상 유지)",
                        zombies.size(), ZOMBIE_THRESHOLD.toHours());
                zombies.forEach(z -> log.warn("  - orderId={}, status={}, orderedAt={}",
                        z.get("id"), z.get("order_status"), z.get("ordered_at")));
            } else {
                log.info("좀비 주문 없음");
            }
        } catch (Exception e) {
            log.warn("좀비 주문 검증 스킵 (테이블 미존재 가능): {}", e.getMessage());
        }
    }

    private void checkOrderPaymentConsistency() {
        try {
            List<Map<String, Object>> inconsistent = jdbcTemplate.queryForList(
                    "SELECT o.id as order_id, o.order_status, p.status as payment_status " +
                    "FROM order_schema.orders o " +
                    "LEFT JOIN payment_schema.payments p ON o.id = p.order_id " +
                    "WHERE (o.order_status = 'PAID' AND (p.status IS NULL OR p.status != 'COMPLETED')) " +
                    "   OR (o.order_status = 'CANCELLED' AND p.status = 'COMPLETED')"
            );

            if (!inconsistent.isEmpty()) {
                log.error("주문-결제 정합성 불일치: {}건", inconsistent.size());
                inconsistent.forEach(r -> log.error("  - orderId={}, orderStatus={}, paymentStatus={}",
                        r.get("order_id"), r.get("order_status"), r.get("payment_status")));
            } else {
                log.info("주문-결제 정합성 정상");
            }
        } catch (Exception e) {
            log.warn("주문-결제 정합성 검증 스킵 (테이블 미존재 가능): {}", e.getMessage());
        }
    }
}
