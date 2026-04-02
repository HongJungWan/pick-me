-- V2: 모듈별 Outbox + Processed Events 테이블 (8개 스키마 × 2 테이블)

-- ─── order_schema ───
CREATE TABLE order_schema.outbox_events (
    id              BIGSERIAL PRIMARY KEY,
    event_id        UUID NOT NULL UNIQUE,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(200) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    published       BOOLEAN NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMP,
    retry_count     INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_order_outbox_unpublished ON order_schema.outbox_events (published, created_at) WHERE published = FALSE;

CREATE TABLE order_schema.processed_events (
    event_id     UUID PRIMARY KEY,
    event_type   VARCHAR(200) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ─── payment_schema ───
CREATE TABLE payment_schema.outbox_events (
    id              BIGSERIAL PRIMARY KEY,
    event_id        UUID NOT NULL UNIQUE,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(200) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    published       BOOLEAN NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMP,
    retry_count     INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_payment_outbox_unpublished ON payment_schema.outbox_events (published, created_at) WHERE published = FALSE;

CREATE TABLE payment_schema.processed_events (
    event_id     UUID PRIMARY KEY,
    event_type   VARCHAR(200) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ─── product_schema ───
CREATE TABLE product_schema.outbox_events (
    id              BIGSERIAL PRIMARY KEY,
    event_id        UUID NOT NULL UNIQUE,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(200) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    published       BOOLEAN NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMP,
    retry_count     INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_product_outbox_unpublished ON product_schema.outbox_events (published, created_at) WHERE published = FALSE;

CREATE TABLE product_schema.processed_events (
    event_id     UUID PRIMARY KEY,
    event_type   VARCHAR(200) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ─── inventory_schema ───
CREATE TABLE inventory_schema.outbox_events (
    id              BIGSERIAL PRIMARY KEY,
    event_id        UUID NOT NULL UNIQUE,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(200) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    published       BOOLEAN NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMP,
    retry_count     INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_inventory_outbox_unpublished ON inventory_schema.outbox_events (published, created_at) WHERE published = FALSE;

CREATE TABLE inventory_schema.processed_events (
    event_id     UUID PRIMARY KEY,
    event_type   VARCHAR(200) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ─── member_schema ───
CREATE TABLE member_schema.outbox_events (
    id              BIGSERIAL PRIMARY KEY,
    event_id        UUID NOT NULL UNIQUE,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(200) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    published       BOOLEAN NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMP,
    retry_count     INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_member_outbox_unpublished ON member_schema.outbox_events (published, created_at) WHERE published = FALSE;

CREATE TABLE member_schema.processed_events (
    event_id     UUID PRIMARY KEY,
    event_type   VARCHAR(200) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ─── partner_schema ───
CREATE TABLE partner_schema.outbox_events (
    id              BIGSERIAL PRIMARY KEY,
    event_id        UUID NOT NULL UNIQUE,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(200) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    published       BOOLEAN NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMP,
    retry_count     INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_partner_outbox_unpublished ON partner_schema.outbox_events (published, created_at) WHERE published = FALSE;

CREATE TABLE partner_schema.processed_events (
    event_id     UUID PRIMARY KEY,
    event_type   VARCHAR(200) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ─── notification_schema ───
CREATE TABLE notification_schema.outbox_events (
    id              BIGSERIAL PRIMARY KEY,
    event_id        UUID NOT NULL UNIQUE,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(200) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    published       BOOLEAN NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMP,
    retry_count     INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_notification_outbox_unpublished ON notification_schema.outbox_events (published, created_at) WHERE published = FALSE;

CREATE TABLE notification_schema.processed_events (
    event_id     UUID PRIMARY KEY,
    event_type   VARCHAR(200) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ─── settlement_schema ───
CREATE TABLE settlement_schema.outbox_events (
    id              BIGSERIAL PRIMARY KEY,
    event_id        UUID NOT NULL UNIQUE,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(200) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    published       BOOLEAN NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMP,
    retry_count     INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_settlement_outbox_unpublished ON settlement_schema.outbox_events (published, created_at) WHERE published = FALSE;

CREATE TABLE settlement_schema.processed_events (
    event_id     UUID PRIMARY KEY,
    event_type   VARCHAR(200) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW()
);
