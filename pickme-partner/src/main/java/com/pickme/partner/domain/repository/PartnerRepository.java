package com.pickme.partner.domain.repository;

import com.pickme.partner.domain.model.Partner;
import com.pickme.partner.domain.model.PartnerId;
import java.util.Optional;

public interface PartnerRepository {
    Partner save(Partner partner);
    Optional<Partner> findById(PartnerId partnerId);
}
