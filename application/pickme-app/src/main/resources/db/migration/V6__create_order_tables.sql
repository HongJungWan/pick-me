-- V6: 주문 도메인 테이블

CREATE TABLE order_schema.orders (
    id              UUID PRIMARY KEY,
    orderer_id      UUID NOT NULL,
    order_status    VARCHAR(20) NOT NULL DEFAULT 'PLACED',
    total_amount    BIGINT NOT NULL DEFAULT 0,
    receiver_name   VARCHAR(50) NOT NULL,
    receiver_phone  VARCHAR(20) NOT NULL,
    zip_code        VARCHAR(10) NOT NULL,
    road_address    VARCHAR(200) NOT NULL,
    address_detail  VARCHAR(200),
    ordered_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE order_schema.order_lines (
    id              BIGSERIAL PRIMARY KEY,
    order_id        UUID NOT NULL REFERENCES order_schema.orders(id),
    product_id      UUID NOT NULL,
    product_name    VARCHAR(200) NOT NULL,
    quantity        INT NOT NULL,
    unit_price      BIGINT NOT NULL,
    line_total      BIGINT NOT NULL
);

CREATE INDEX idx_orders_orderer_status ON order_schema.orders (orderer_id, order_status);
CREATE INDEX idx_orders_ordered_at ON order_schema.orders (ordered_at);
CREATE INDEX idx_order_lines_order ON order_schema.order_lines (order_id);
