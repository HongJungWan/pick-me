package com.pickme.orchestration.monitoring;

import com.pickme.common.dlt.SlackNotifier;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsRequest;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsResponse;
import io.temporal.client.WorkflowClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Temporal 워크플로우 실패/타임아웃 모니터링.
 * Kafka DeadLetterConsumer + SlackNotifier에 대응하는 Temporal 측 감시.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pickme.temporal.enabled", havingValue = "true")
public class TemporalFailureMonitor {

    private final WorkflowClient workflowClient;
    @Nullable
    private final SlackNotifier slackNotifier;

    @Scheduled(fixedDelay = 300000) // 5분마다
    public void checkFailedWorkflows() {
        try {
            ListWorkflowExecutionsResponse failed = workflowClient.getWorkflowServiceStubs()
                    .blockingStub()
                    .listWorkflowExecutions(ListWorkflowExecutionsRequest.newBuilder()
                            .setNamespace(workflowClient.getOptions().getNamespace())
                            .setQuery("ExecutionStatus = 'Failed' OR ExecutionStatus = 'TimedOut'")
                            .setPageSize(50)
                            .build());

            int failedCount = failed.getExecutionsCount();
            if (failedCount > 0) {
                log.error("실패/타임아웃 워크플로우 감지: {}건", failedCount);

                StringBuilder alertMessage = new StringBuilder(
                        ":rotating_light: *Temporal 워크플로우 실패 감지* — " + failedCount + "건\n");

                failed.getExecutionsList().forEach(exec -> {
                    String detail = String.format("- workflowId: `%s`, status: `%s`",
                            exec.getExecution().getWorkflowId(), exec.getStatus());
                    log.error("  {}", detail);
                    alertMessage.append(detail).append("\n");
                });

                if (slackNotifier != null) {
                    slackNotifier.sendAlert(alertMessage.toString());
                }
            } else {
                log.debug("실패/타임아웃 워크플로우 없음");
            }
        } catch (Exception e) {
            log.warn("Temporal 실패 워크플로우 모니터링 오류", e);
        }
    }
}
