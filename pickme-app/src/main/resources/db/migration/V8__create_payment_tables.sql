-- V8: 결제 도메인 테이블

CREATE TABLE payment_schema.payments (
    id                  UUID PRIMARY KEY,
    order_id            UUID NOT NULL,
    payer_id            UUID NOT NULL,
    amount              BIGINT NOT NULL,
    payment_method      VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    pg_transaction_id   VARCHAR(100),
    paid_at             TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_order_id ON payment_schema.payments (order_id);
CREATE INDEX idx_payments_payer_id ON payment_schema.payments (payer_id);
