package com.pickme.partner.application;

import com.pickme.common.event.DomainEventPublisher;
import com.pickme.partner.domain.model.BusinessInfo;
import com.pickme.partner.domain.model.ContractInfo;
import com.pickme.partner.domain.model.Partner;
import com.pickme.partner.domain.model.PartnerId;
import com.pickme.partner.domain.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final DomainEventPublisher eventPublisher;

    @Transactional
    public Partner registerPartner(String registrationNumber, String companyName, String representativeName,
                                   BigDecimal commissionRate, String settlementCycle,
                                   LocalDate contractStartDate, LocalDate contractEndDate) {
        BusinessInfo businessInfo = new BusinessInfo(registrationNumber, companyName, representativeName);
        ContractInfo contractInfo = new ContractInfo(commissionRate, settlementCycle, contractStartDate, contractEndDate);
        Partner partner = Partner.register(businessInfo, contractInfo);
        Partner saved = partnerRepository.save(partner);
        eventPublisher.publishAll(partner);
        return saved;
    }

    @Transactional(readOnly = true)
    public Partner getPartner(UUID partnerId) {
        return partnerRepository.findById(PartnerId.of(partnerId))
                .orElseThrow(() -> new IllegalArgumentException("파트너를 찾을 수 없습니다: " + partnerId));
    }

    @Transactional
    public Partner approvePartner(UUID partnerId) {
        Partner partner = getPartner(partnerId);
        partner.approve();
        partnerRepository.save(partner);
        eventPublisher.publishAll(partner);
        return partner;
    }
}
