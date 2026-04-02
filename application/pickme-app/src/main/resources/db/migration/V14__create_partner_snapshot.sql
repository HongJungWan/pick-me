-- V14: 정산 모듈 파트너 스냅샷 테이블

CREATE TABLE settlement_schema.partner_snapshot (
    partner_id       UUID PRIMARY KEY,
    company_name     VARCHAR(100) NOT NULL,
    commission_rate  DECIMAL(5,2) NOT NULL DEFAULT 0,
    settlement_cycle VARCHAR(20),
    status           VARCHAR(15) NOT NULL,
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);
