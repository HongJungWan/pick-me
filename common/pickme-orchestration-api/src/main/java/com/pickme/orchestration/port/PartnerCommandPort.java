package com.pickme.orchestration.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 파트너 도메인에 대한 명령 포트.
 * Temporal Activity가 이 인터페이스를 통해 파트너 등록/승인을 요청한다.
 */
public interface PartnerCommandPort {

    UUID registerPartner(String registrationNumber, String companyName, String representativeName,
                         BigDecimal commissionRate, String settlementCycle,
                         LocalDate contractStartDate, LocalDate contractEndDate);

    void approvePartner(UUID partnerId);

    void rejectPartner(UUID partnerId, String reason);
}
