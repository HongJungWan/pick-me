package com.pickme.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FallbackController {

    @GetMapping("/fallback")
    public ResponseEntity<Map<String, Object>> getFallback() {
        return fallbackResponse();
    }

    @PostMapping("/fallback")
    public ResponseEntity<Map<String, Object>> postFallback() {
        return fallbackResponse();
    }

    private ResponseEntity<Map<String, Object>> fallbackResponse() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "success", false,
                        "error", Map.of(
                                "code", "SERVICE_UNAVAILABLE",
                                "message", "서비스가 일시적으로 불가합니다. 잠시 후 다시 시도해주세요."
                        )
                ));
    }
}
