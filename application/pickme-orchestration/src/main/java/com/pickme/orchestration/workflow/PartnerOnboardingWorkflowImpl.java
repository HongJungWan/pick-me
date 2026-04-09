package com.pickme.orchestration.workflow;

import com.pickme.orchestration.activity.PartnerOnboardingActivities;
import com.pickme.orchestration.dto.PartnerOnboardingRequest;
import com.pickme.orchestration.dto.PartnerOnboardingResult;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.UUID;

public class PartnerOnboardingWorkflowImpl implements PartnerOnboardingWorkflow {

    private final PartnerOnboardingActivities activities = Workflow.newActivityStub(
            PartnerOnboardingActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .build())
                    .build()
    );

    private String status = "STARTED";
    private boolean approved = false;
    private boolean rejected = false;
    private String rejectionReason = null;
    private UUID partnerId = null;

    @Override
    public PartnerOnboardingResult execute(PartnerOnboardingRequest request) {
        // Step 1: 파트너 등록 (PENDING 상태)
        status = "REGISTERING";
        try {
            partnerId = activities.registerPartner(request);
        } catch (ActivityFailure e) {
            status = "REGISTRATION_FAILED";
            return PartnerOnboardingResult.rejected(null, "등록 실패: " + e.getMessage());
        }

        if (partnerId == null) {
            status = "REGISTRATION_FAILED";
            return PartnerOnboardingResult.rejected(null, "등록 실패: partnerId 반환 없음");
        }

        // Step 2: 인간 승인 대기 (최대 7일)
        status = "WAITING_APPROVAL";
        boolean signalReceived = Workflow.await(Duration.ofDays(7), () -> approved || rejected);

        if (!signalReceived) {
            // 7일 타임아웃 → 자동 만료
            status = "EXPIRED";
            activities.notifyExpired(partnerId);
            return PartnerOnboardingResult.expired(partnerId);
        }

        if (rejected) {
            // 관리자 거절
            status = "REJECTED";
            activities.rejectPartner(partnerId, rejectionReason);
            return PartnerOnboardingResult.rejected(partnerId, rejectionReason);
        }

        // Step 3: 승인 처리
        status = "APPROVING";
        try {
            activities.approvePartner(partnerId);
        } catch (ActivityFailure e) {
            status = "APPROVAL_FAILED";
            return PartnerOnboardingResult.rejected(partnerId, "승인 처리 실패: " + e.getMessage());
        }

        status = "COMPLETED";
        return PartnerOnboardingResult.approved(partnerId);
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void approveByAdmin() {
        this.approved = true;
    }

    @Override
    public void rejectByAdmin(String reason) {
        this.rejected = true;
        this.rejectionReason = reason;
    }
}
