-- V3: 상품 도메인 테이블

CREATE TABLE product_schema.products (
    id              UUID PRIMARY KEY,
    partner_id      UUID NOT NULL,
    product_name    VARCHAR(200) NOT NULL,
    description     TEXT,
    base_price      BIGINT NOT NULL DEFAULT 0,
    discounted_price BIGINT NOT NULL DEFAULT 0,
    category_code   VARCHAR(50) NOT NULL,
    category_name   VARCHAR(100) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE product_schema.product_options (
    id              BIGSERIAL PRIMARY KEY,
    product_id      UUID NOT NULL REFERENCES product_schema.products(id),
    option_name     VARCHAR(100) NOT NULL,
    option_value    VARCHAR(200) NOT NULL,
    additional_price BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_products_status ON product_schema.products (status);
CREATE INDEX idx_products_partner ON product_schema.products (partner_id);
CREATE INDEX idx_product_options_product ON product_schema.product_options (product_id);
