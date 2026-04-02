-- V10: 파트너 도메인 테이블

CREATE TABLE partner_schema.partners (
    id                      UUID PRIMARY KEY,
    registration_number     VARCHAR(20) NOT NULL UNIQUE,
    company_name            VARCHAR(100) NOT NULL,
    representative_name     VARCHAR(50),
    commission_rate         DECIMAL(5,2) NOT NULL DEFAULT 0,
    settlement_cycle        VARCHAR(20),
    contract_start_date     DATE,
    contract_end_date       DATE,
    status                  VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);
