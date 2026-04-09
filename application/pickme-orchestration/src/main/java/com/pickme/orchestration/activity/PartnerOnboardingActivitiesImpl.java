package com.pickme.orchestration.activity;

import com.pickme.orchestration.dto.PartnerOnboardingRequest;
import com.pickme.orchestration.port.PartnerCommandPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PartnerOnboardingActivitiesImpl implements PartnerOnboardingActivities {

    private final PartnerCommandPort partnerCommandPort;

    @Override
    public UUID registerPartner(PartnerOnboardingRequest request) {
        log.info("[Activity] 파트너 등록: company={}", request.companyName());
        return partnerCommandPort.registerPartner(
                request.registrationNumber(), request.companyName(), request.representativeName(),
                request.commissionRate(), request.settlementCycle(),
                request.contractStartDate(), request.contractEndDate());
    }

    @Override
    public void approvePartner(UUID partnerId) {
        log.info("[Activity] 파트너 승인: partnerId={}", partnerId);
        partnerCommandPort.approvePartner(partnerId);
    }

    @Override
    public void rejectPartner(UUID partnerId, String reason) {
        log.info("[Activity] 파트너 거절: partnerId={}, reason={}", partnerId, reason);
        partnerCommandPort.rejectPartner(partnerId, reason);
    }

    @Override
    public void notifyExpired(UUID partnerId) {
        log.warn("[Activity] 파트너 온보딩 만료 (7일 초과): partnerId={}", partnerId);
        // TODO: NotificationGateway 연동 — 만료 알림 발송
    }
}
