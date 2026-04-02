package com.pickme.partner.domain.model;

import com.pickme.common.event.DomainEvent;
import com.pickme.partner.domain.event.PartnerApprovedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Partner {
    private final PartnerId partnerId;
    private final BusinessInfo businessInfo;
    private ContractInfo contractInfo;
    private PartnerStatus status;
    private final List<DomainEvent> domainEvents;

    private Partner(PartnerId partnerId, BusinessInfo businessInfo, ContractInfo contractInfo, PartnerStatus status) {
        this.partnerId = partnerId; this.businessInfo = businessInfo;
        this.contractInfo = contractInfo; this.status = status;
        this.domainEvents = new ArrayList<>();
    }

    public static Partner register(BusinessInfo businessInfo, ContractInfo contractInfo) {
        return new Partner(PartnerId.generate(), businessInfo, contractInfo, PartnerStatus.PENDING);
    }

    public static Partner reconstitute(PartnerId id, BusinessInfo businessInfo, ContractInfo contractInfo, PartnerStatus status) {
        return new Partner(id, businessInfo, contractInfo, status);
    }

    public void approve() {
        if (this.status != PartnerStatus.PENDING) throw new IllegalStateException("대기 상태에서만 승인 가능합니다");
        this.status = PartnerStatus.APPROVED;
        domainEvents.add(new PartnerApprovedEvent(partnerId.getValue(), businessInfo.getCompanyName()));
    }

    public void suspend(String reason) {
        if (this.status != PartnerStatus.APPROVED) throw new IllegalStateException("승인 상태에서만 정지 가능합니다");
        this.status = PartnerStatus.SUSPENDED;
    }

    public PartnerId getPartnerId() { return partnerId; }
    public BusinessInfo getBusinessInfo() { return businessInfo; }
    public ContractInfo getContractInfo() { return contractInfo; }
    public PartnerStatus getStatus() { return status; }
    public List<DomainEvent> getDomainEvents() { return Collections.unmodifiableList(domainEvents); }
    public void clearDomainEvents() { domainEvents.clear(); }
}
