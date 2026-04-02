package com.pickme.partner.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickme.common.event.DomainEvent;
import com.pickme.common.outbox.OutboxEvent;
import com.pickme.common.outbox.OutboxRepository;
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
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Partner registerPartner(String registrationNumber, String companyName, String representativeName,
                                   BigDecimal commissionRate, String settlementCycle,
                                   LocalDate contractStartDate, LocalDate contractEndDate) {
        BusinessInfo businessInfo = new BusinessInfo(registrationNumber, companyName, representativeName);
        ContractInfo contractInfo = new ContractInfo(commissionRate, settlementCycle, contractStartDate, contractEndDate);
        Partner partner = Partner.register(businessInfo, contractInfo);
        return partnerRepository.save(partner);
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
        publishDomainEvents(partner);
        return partner;
    }

    private void publishDomainEvents(Partner partner) {
        for (DomainEvent event : partner.getDomainEvents()) {
            try {
                String payload = objectMapper.writeValueAsString(event);
                outboxRepository.save(OutboxEvent.from(event, payload));
            } catch (JsonProcessingException e) { throw new RuntimeException("이벤트 직렬화 실패", e); }
        }
        partner.clearDomainEvents();
    }
}
