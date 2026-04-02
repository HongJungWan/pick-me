package com.pickme.partner.infrastructure.external;

import java.util.UUID;

/**
 * 택배사 외부 API Anti-Corruption Layer
 * 외부 택배사 API 모델을 내부 도메인 모델로 변환
 */
public interface DeliveryGateway {

    DeliveryResult requestDelivery(UUID orderId, String receiverName, String address, String phone);

    DeliveryTrackingInfo getTrackingInfo(String trackingNumber);

    record DeliveryResult(String trackingNumber, boolean success, String message) {
        public static DeliveryResult success(String trackingNumber) {
            return new DeliveryResult(trackingNumber, true, "배송 접수 완료");
        }
        public static DeliveryResult failure(String message) {
            return new DeliveryResult(null, false, message);
        }
    }

    record DeliveryTrackingInfo(String trackingNumber, String status, String currentLocation) {}
}
