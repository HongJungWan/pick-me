package com.pickme.partner.infrastructure.external;

/**
 * 외부 알림 서비스 Anti-Corruption Layer (카카오 알림톡 등)
 * 외부 API 모델을 내부 도메인 모델로 변환
 */
public interface NotificationGateway {

    SendResult sendKakaoAlimtalk(String phone, String templateCode, String content);

    SendResult sendSms(String phone, String content);

    record SendResult(boolean success, String messageId, String message) {
        public static SendResult success(String messageId) {
            return new SendResult(true, messageId, "발송 성공");
        }
        public static SendResult failure(String message) {
            return new SendResult(false, null, message);
        }
    }
}
