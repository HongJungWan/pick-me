package com.pickme.orchestration.starter;

import com.pickme.orchestration.dto.OrderLineItem;
import com.pickme.orchestration.dto.PartnerOnboardingRequest;
import com.pickme.orchestration.port.WorkflowStarter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Temporal 비활성화 시 사용되는 NoOp 폴백.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "pickme.temporal.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpWorkflowStarter implements WorkflowStarter {

    @Override
    public void startOrderFulfillment(UUID orderId, UUID ordererId,
                                      List<OrderLineItem> orderLines, long totalAmount, String paymentMethod) {
        log.debug("Temporal 비활성화 — 주문 이행 워크플로우 시작 생략: orderId={}", orderId);
    }

    @Override
    public void startRefund(UUID orderId, String reason, long refundAmount, List<OrderLineItem> orderLines) {
        log.debug("Temporal 비활성화 — 환불 워크플로우 시작 생략: orderId={}", orderId);
    }

    @Override
    public void startSettlementReconciliation(LocalDate date) {
        log.debug("Temporal 비활성화 — 정산 워크플로우 시작 생략: date={}", date);
    }

    @Override
    public void startPartnerOnboarding(PartnerOnboardingRequest request) {
        log.debug("Temporal 비활성화 — 파트너 온보딩 워크플로우 시작 생략: company={}", request.companyName());
    }
}
