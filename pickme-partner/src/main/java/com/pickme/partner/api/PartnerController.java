package com.pickme.partner.api;

import com.pickme.partner.api.request.CreatePartnerRequest;
import com.pickme.partner.api.response.PartnerResponse;
import com.pickme.partner.application.PartnerService;
import com.pickme.partner.domain.model.Partner;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    @PostMapping
    public ResponseEntity<PartnerResponse> createPartner(@Valid @RequestBody CreatePartnerRequest request) {
        Partner partner = partnerService.registerPartner(
                request.registrationNumber(), request.companyName(), request.representativeName(),
                request.commissionRate(), request.settlementCycle(),
                request.contractStartDate(), request.contractEndDate()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(PartnerResponse.from(partner));
    }

    @GetMapping("/{partnerId}")
    public ResponseEntity<PartnerResponse> getPartner(@PathVariable UUID partnerId) {
        return ResponseEntity.ok(PartnerResponse.from(partnerService.getPartner(partnerId)));
    }

    @PostMapping("/{partnerId}/approve")
    public ResponseEntity<PartnerResponse> approvePartner(@PathVariable UUID partnerId) {
        return ResponseEntity.ok(PartnerResponse.from(partnerService.approvePartner(partnerId)));
    }
}
