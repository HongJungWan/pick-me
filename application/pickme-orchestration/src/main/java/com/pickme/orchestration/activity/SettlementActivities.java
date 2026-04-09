package com.pickme.orchestration.activity;

import com.pickme.orchestration.port.SettlementCommandPort.PartnerSettlementInfo;
import com.pickme.orchestration.port.SettlementCommandPort.ReconciliationResult;
import io.temporal.activity.ActivityInterface;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ActivityInterface
public interface SettlementActivities {

    List<PartnerSettlementInfo> fetchDailySnapshots(LocalDate date);

    ReconciliationResult reconcilePartner(UUID partnerId, LocalDate date);

    void reportDiscrepancies(LocalDate date, List<String> discrepancies);
}
