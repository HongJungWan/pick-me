package com.pickme.order.api;

import com.pickme.common.ratelimit.RateLimiter;
import com.pickme.order.api.request.CreateOrderRequest;
import com.pickme.order.api.response.OrderResponse;
import com.pickme.order.application.OrderService;
import com.pickme.order.domain.model.Order;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @RateLimiter(key = "'order:' + #request.ordererId()", limit = 10, windowSeconds = 60)
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
        Order order = orderService.getOrder(orderId);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(@RequestParam UUID ordererId) {
        List<OrderResponse> orders = orderService.getOrdersByOrderer(ordererId).stream()
                .map(OrderResponse::from)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID orderId,
                                                     @RequestBody Map<String, String> body) {
        Order order = orderService.cancelOrder(orderId, body.getOrDefault("reason", "고객 요청 취소"));
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
