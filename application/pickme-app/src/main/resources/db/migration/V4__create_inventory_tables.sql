-- V4: 재고 도메인 테이블

CREATE TABLE inventory_schema.stocks (
    id                  UUID PRIMARY KEY,
    product_id          UUID NOT NULL UNIQUE,
    quantity            INT NOT NULL DEFAULT 0,
    reserved_quantity   INT NOT NULL DEFAULT 0,
    total_quantity      INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_stocks_product_id ON inventory_schema.stocks (product_id);

CREATE TABLE inventory_schema.stock_history (
    id              BIGSERIAL PRIMARY KEY,
    stock_id        UUID NOT NULL REFERENCES inventory_schema.stocks(id),
    operation       VARCHAR(20) NOT NULL,
    quantity_change INT NOT NULL,
    order_id        UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_history_stock ON inventory_schema.stock_history (stock_id);
