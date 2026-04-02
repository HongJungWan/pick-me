package com.pickme.payment.api;

import com.pickme.payment.api.response.PaymentResponse;
import com.pickme.payment.application.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(PaymentResponse.from(paymentService.getPayment(paymentId)));
    }

    @GetMapping
    public ResponseEntity<PaymentResponse> getPaymentByOrder(@RequestParam UUID orderId) {
        return ResponseEntity.ok(PaymentResponse.from(paymentService.getPaymentByOrderId(orderId)));
    }
}
