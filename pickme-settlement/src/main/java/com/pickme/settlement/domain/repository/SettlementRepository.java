package com.pickme.settlement.domain.repository;

import com.pickme.settlement.domain.model.Settlement;
import com.pickme.settlement.domain.model.SettlementId;
import com.pickme.settlement.domain.model.SettlementPeriod;

import java.util.Optional;
import java.util.UUID;

public interface SettlementRepository {

    Settlement save(Settlement settlement);

    Optional<Settlement> findById(SettlementId id);

    Optional<Settlement> findByPartnerIdAndPeriod(UUID partnerId, SettlementPeriod period);
}
