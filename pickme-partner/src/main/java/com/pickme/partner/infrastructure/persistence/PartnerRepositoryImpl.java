package com.pickme.partner.infrastructure.persistence;

import com.pickme.partner.domain.model.BusinessInfo;
import com.pickme.partner.domain.model.ContractInfo;
import com.pickme.partner.domain.model.Partner;
import com.pickme.partner.domain.model.PartnerId;
import com.pickme.partner.domain.model.PartnerStatus;
import com.pickme.partner.domain.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PartnerRepositoryImpl implements PartnerRepository {

    private final JpaPartnerRepository jpaRepository;

    @Override
    public Partner save(Partner p) {
        PartnerJpaEntity entity = new PartnerJpaEntity(
                p.getPartnerId().getValue(), p.getBusinessInfo().getRegistrationNumber(),
                p.getBusinessInfo().getCompanyName(), p.getBusinessInfo().getRepresentativeName(),
                p.getContractInfo().getCommissionRate(), p.getContractInfo().getSettlementCycle(),
                p.getContractInfo().getContractStartDate(), p.getContractInfo().getContractEndDate(),
                PartnerJpaEntity.StatusJpa.valueOf(p.getStatus().name())
        );
        jpaRepository.save(entity);
        return p;
    }

    @Override
    public Optional<Partner> findById(PartnerId partnerId) {
        return jpaRepository.findById(partnerId.getValue()).map(e -> Partner.reconstitute(
                PartnerId.of(e.getId()),
                new BusinessInfo(e.getRegistrationNumber(), e.getCompanyName(), e.getRepresentativeName()),
                new ContractInfo(e.getCommissionRate(), e.getSettlementCycle(), e.getContractStartDate(), e.getContractEndDate()),
                PartnerStatus.valueOf(e.getStatus().name())
        ));
    }
}
