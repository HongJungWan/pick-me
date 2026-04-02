package com.pickme.settlement.application;

import com.pickme.common.idempotency.IdempotencyFilter;
import com.pickme.settlement.infrastructure.snapshot.PartnerSnapshotRepository;
import com.pickme.settlement.infrastructure.snapshot.SalesSnapshotEntity;
import com.pickme.settlement.infrastructure.snapshot.SalesSnapshotRepository;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementEventHandler {

    private final SalesSnapshotRepository salesSnapshotRepository;
    private final PartnerSnapshotRepository partnerSnapshotRepository;
    private final IdempotencyFilter idempotencyFilter;

    @Transactional
    public void handlePaymentCompleted(UUID eventId, UUID payerId, long amount) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        LocalDate today = LocalDate.now();
        SalesSnapshotEntity snapshot = salesSnapshotRepository.findOrCreate(today, payerId);
        snapshot.addSale(amount);
        salesSnapshotRepository.save(snapshot);

        idempotencyFilter.markProcessed(eventId, "PaymentCompletedEvent");
        log.info("매출 스냅샷 누적: date={}, partnerId={}, amount={}", today, payerId, amount);
    }

    @Transactional
    public void handleRefundCompleted(UUID eventId, UUID payerId, long refundAmount) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        LocalDate today = LocalDate.now();
        SalesSnapshotEntity snapshot = salesSnapshotRepository.findOrCreate(today, payerId);
        snapshot.addRefund(refundAmount);
        salesSnapshotRepository.save(snapshot);

        idempotencyFilter.markProcessed(eventId, "RefundCompletedEvent");
        log.info("환불 스냅샷 반영: date={}, partnerId={}, refundAmount={}", today, payerId, refundAmount);
    }

    @Transactional
    public void handlePartnerApproved(UUID eventId, UUID partnerId, String companyName,
                                      BigDecimal commissionRate, String settlementCycle) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        partnerSnapshotRepository.upsert(partnerId, companyName, commissionRate, settlementCycle, "APPROVED");

        idempotencyFilter.markProcessed(eventId, "PartnerApprovedEvent");
        log.info("파트너 스냅샷 생성: partnerId={}, companyName={}", partnerId, companyName);
    }
}
