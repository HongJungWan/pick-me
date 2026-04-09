package com.pickme.common.dlt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "pickme.slack.webhook-url")
public class SlackNotifier {

    private final RestTemplate restTemplate;
    private final String webhookUrl;

    public SlackNotifier(@Value("${pickme.slack.webhook-url}") String webhookUrl) {
        this.restTemplate = new RestTemplate();
        this.webhookUrl = webhookUrl;
    }

    public void sendAlert(String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("text", message), headers);
            restTemplate.postForEntity(webhookUrl, request, String.class);
            log.info("Slack 알림 발송 완료");
        } catch (Exception e) {
            log.warn("Slack 알림 발송 실패 (서비스 영향 없음): {}", e.getMessage());
        }
    }

    public void notifyDeadLetterEvent(DeadLetterEvent event) {
        try {
            String message = String.format(
                    ":rotating_light: *DLT 이벤트 적재*\n" +
                    "- eventId: `%s`\n" +
                    "- eventType: `%s`\n" +
                    "- error: %s\n" +
                    "- 재처리: `POST /api/v1/admin/dlt/%s/retry`",
                    event.getEventId(), event.getEventType(),
                    event.getErrorMessage(), event.getEventId()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("text", message), headers);

            restTemplate.postForEntity(webhookUrl, request, String.class);
            log.info("Slack DLT 알림 발송: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.warn("Slack 알림 발송 실패 (서비스 영향 없음): {}", e.getMessage());
        }
    }
}
