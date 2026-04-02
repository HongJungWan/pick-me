package com.pickme.partner.api.response;

import com.pickme.partner.domain.model.Partner;
import java.math.BigDecimal;
import java.util.UUID;

public record PartnerResponse(
        UUID partnerId, String registrationNumber, String companyName,
        String representativeName, BigDecimal commissionRate, String status
) {
    public static PartnerResponse from(Partner p) {
        return new PartnerResponse(
                p.getPartnerId().getValue(), p.getBusinessInfo().getRegistrationNumber(),
                p.getBusinessInfo().getCompanyName(), p.getBusinessInfo().getRepresentativeName(),
                p.getContractInfo().getCommissionRate(), p.getStatus().name()
        );
    }
}
