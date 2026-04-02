-- V7: 주문 모듈 CQRS Read Model (스냅샷)

CREATE TABLE order_schema.product_snapshot (
    product_id      UUID PRIMARY KEY,
    product_name    VARCHAR(200) NOT NULL,
    selling_price   BIGINT NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE order_schema.member_snapshot (
    member_id       UUID PRIMARY KEY,
    name            VARCHAR(50) NOT NULL,
    grade           VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
