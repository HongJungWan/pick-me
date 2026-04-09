package com.pickme.partner.application;

import com.pickme.common.idempotency.IdempotencyFilter;
import com.pickme.orchestration.port.PartnerCommandPort;
import com.pickme.partner.domain.model.Partner;
import com.pickme.partner.domain.model.PartnerId;
import com.pickme.partner.domain.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerCommandAdapter implements PartnerCommandPort {

    private final PartnerService partnerService;
    private final PartnerRepository partnerRepository;
    private final IdempotencyFilter idempotencyFilter;

    @Transactional
    @Override
    public UUID registerPartner(String registrationNumber, String companyName, String representativeName,
                                BigDecimal commissionRate, String settlementCycle,
                                LocalDate contractStartDate, LocalDate contractEndDate) {
        UUID idempotencyKey = UUID.nameUUIDFromBytes(
                ("temporal-register-partner:" + registrationNumber).getBytes(StandardCharsets.UTF_8));

        if (idempotencyFilter.isDuplicate(idempotencyKey)) {
            log.info("중복 Activity 무시 (registerPartner): registrationNumber={}", registrationNumber);
            return null;
        }

        Partner partner = partnerService.registerPartner(
                registrationNumber, companyName, representativeName,
                commissionRate, settlementCycle, contractStartDate, contractEndDate);

        idempotencyFilter.markProcessed(idempotencyKey, "TemporalRegisterPartner");
        log.info("[CommandPort] 파트너 등록: partnerId={}", partner.getPartnerId().getValue());
        return partner.getPartnerId().getValue();
    }

    @Transactional
    @Override
    public void approvePartner(UUID partnerId) {
        UUID idempotencyKey = UUID.nameUUIDFromBytes(
                ("temporal-approve-partner:" + partnerId).getBytes(StandardCharsets.UTF_8));

        if (idempotencyFilter.isDuplicate(idempotencyKey)) {
            log.info("중복 Activity 무시 (approvePartner): partnerId={}", partnerId);
            return;
        }

        partnerService.approvePartner(partnerId);
        idempotencyFilter.markProcessed(idempotencyKey, "TemporalApprovePartner");
        log.info("[CommandPort] 파트너 승인: partnerId={}", partnerId);
    }

    @Transactional
    @Override
    public void rejectPartner(UUID partnerId, String reason) {
        UUID idempotencyKey = UUID.nameUUIDFromBytes(
                ("temporal-reject-partner:" + partnerId).getBytes(StandardCharsets.UTF_8));

        if (idempotencyFilter.isDuplicate(idempotencyKey)) {
            log.info("중복 Activity 무시 (rejectPartner): partnerId={}", partnerId);
            return;
        }

        Partner partner = partnerRepository.findById(PartnerId.of(partnerId))
                .orElseThrow(() -> new IllegalArgumentException("파트너를 찾을 수 없습니다: " + partnerId));

        partner.suspend(reason);
        partnerRepository.save(partner);

        idempotencyFilter.markProcessed(idempotencyKey, "TemporalRejectPartner");
        log.info("[CommandPort] 파트너 거절: partnerId={}, reason={}", partnerId, reason);
    }
}
