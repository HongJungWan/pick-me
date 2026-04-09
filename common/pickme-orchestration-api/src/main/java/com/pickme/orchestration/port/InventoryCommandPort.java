package com.pickme.orchestration.port;

import com.pickme.orchestration.dto.OrderLineItem;
import com.pickme.orchestration.dto.ReserveResult;

import java.util.List;
import java.util.UUID;

/**
 * 재고 도메인에 대한 명령 포트.
 * Temporal Activity가 이 인터페이스를 통해 재고 예약/확정/복원을 요청한다.
 */
public interface InventoryCommandPort {

    ReserveResult reserveInventory(UUID orderId, List<OrderLineItem> items);

    void confirmInventory(UUID orderId, List<OrderLineItem> items);

    void restoreInventory(UUID orderId, List<OrderLineItem> items);
}
