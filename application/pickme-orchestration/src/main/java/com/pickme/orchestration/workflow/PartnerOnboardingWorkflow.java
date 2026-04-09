package com.pickme.orchestration.workflow;

import com.pickme.orchestration.dto.PartnerOnboardingRequest;
import com.pickme.orchestration.dto.PartnerOnboardingResult;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * 파트너 온보딩 워크플로우.
 * 등록 → 인간 승인 대기 (최대 7일) → 승인/거절/만료.
 */
@WorkflowInterface
public interface PartnerOnboardingWorkflow {

    @WorkflowMethod
    PartnerOnboardingResult execute(PartnerOnboardingRequest request);

    @QueryMethod
    String getStatus();

    @SignalMethod
    void approveByAdmin();

    @SignalMethod
    void rejectByAdmin(String reason);
}
