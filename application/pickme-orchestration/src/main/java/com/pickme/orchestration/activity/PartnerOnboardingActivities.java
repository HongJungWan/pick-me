package com.pickme.orchestration.activity;

import com.pickme.orchestration.dto.PartnerOnboardingRequest;
import io.temporal.activity.ActivityInterface;

import java.util.UUID;

@ActivityInterface
public interface PartnerOnboardingActivities {

    UUID registerPartner(PartnerOnboardingRequest request);

    void approvePartner(UUID partnerId);

    void rejectPartner(UUID partnerId, String reason);

    void notifyExpired(UUID partnerId);
}
