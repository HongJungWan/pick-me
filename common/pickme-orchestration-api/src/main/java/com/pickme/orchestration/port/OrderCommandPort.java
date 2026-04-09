package com.pickme.orchestration.port;

import java.util.UUID;

/**
 * 주문 도메인에 대한 명령 포트.
 * Temporal Activity가 이 인터페이스를 통해 주문 확정/취소를 요청한다.
 */
public interface OrderCommandPort {

    void confirmOrder(UUID orderId);

    void cancelOrder(UUID orderId, String reason);

    void requestRefund(UUID orderId, String reason);

    void completeRefund(UUID orderId);
}
