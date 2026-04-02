-- V11: 정산 도메인 테이블

CREATE TABLE settlement_schema.daily_sales_aggregate (
    aggregate_date  DATE NOT NULL,
    partner_id      UUID NOT NULL,
    total_orders    INT NOT NULL DEFAULT 0,
    total_sales     BIGINT NOT NULL DEFAULT 0,
    total_refunds   BIGINT NOT NULL DEFAULT 0,
    net_sales       BIGINT NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (aggregate_date, partner_id)
);
