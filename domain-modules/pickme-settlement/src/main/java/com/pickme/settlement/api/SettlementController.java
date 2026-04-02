package com.pickme.settlement.api;

import com.pickme.settlement.api.response.SettlementResponse;
import com.pickme.settlement.application.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public ResponseEntity<List<SettlementResponse>> getSettlements(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<SettlementResponse> settlements;
        if (date != null) {
            settlements = settlementService.getDailySettlements(date).stream()
                    .map(SettlementResponse::from).toList();
        } else {
            settlements = settlementService.getAllSettlements().stream()
                    .map(SettlementResponse::from).toList();
        }
        return ResponseEntity.ok(settlements);
    }
}
