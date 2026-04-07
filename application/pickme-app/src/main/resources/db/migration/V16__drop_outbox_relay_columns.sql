-- V16: Outbox Relay 폴링 방식에서 Debezium CDC로 전환 완료 후 불필요 컬럼 제거
-- Debezium은 WAL을 직접 읽으므로 published/retry_count 컬럼 불필요

-- order_schema
ALTER TABLE order_schema.outbox_events DROP COLUMN IF EXISTS published;
ALTER TABLE order_schema.outbox_events DROP COLUMN IF EXISTS published_at;
ALTER TABLE order_schema.outbox_events DROP COLUMN IF EXISTS retry_count;
DROP INDEX IF EXISTS order_schema.idx_order_outbox_unpublished;

-- payment_schema
ALTER TABLE payment_schema.outbox_events DROP COLUMN IF EXISTS published;
ALTER TABLE payment_schema.outbox_events DROP COLUMN IF EXISTS published_at;
ALTER TABLE payment_schema.outbox_events DROP COLUMN IF EXISTS retry_count;
DROP INDEX IF EXISTS payment_schema.idx_payment_outbox_unpublished;

-- product_schema
ALTER TABLE product_schema.outbox_events DROP COLUMN IF EXISTS published;
ALTER TABLE product_schema.outbox_events DROP COLUMN IF EXISTS published_at;
ALTER TABLE product_schema.outbox_events DROP COLUMN IF EXISTS retry_count;
DROP INDEX IF EXISTS product_schema.idx_product_outbox_unpublished;

-- inventory_schema
ALTER TABLE inventory_schema.outbox_events DROP COLUMN IF EXISTS published;
ALTER TABLE inventory_schema.outbox_events DROP COLUMN IF EXISTS published_at;
ALTER TABLE inventory_schema.outbox_events DROP COLUMN IF EXISTS retry_count;
DROP INDEX IF EXISTS inventory_schema.idx_inventory_outbox_unpublished;

-- member_schema
ALTER TABLE member_schema.outbox_events DROP COLUMN IF EXISTS published;
ALTER TABLE member_schema.outbox_events DROP COLUMN IF EXISTS published_at;
ALTER TABLE member_schema.outbox_events DROP COLUMN IF EXISTS retry_count;
DROP INDEX IF EXISTS member_schema.idx_member_outbox_unpublished;

-- partner_schema
ALTER TABLE partner_schema.outbox_events DROP COLUMN IF EXISTS published;
ALTER TABLE partner_schema.outbox_events DROP COLUMN IF EXISTS published_at;
ALTER TABLE partner_schema.outbox_events DROP COLUMN IF EXISTS retry_count;
DROP INDEX IF EXISTS partner_schema.idx_partner_outbox_unpublished;

-- notification_schema
ALTER TABLE notification_schema.outbox_events DROP COLUMN IF EXISTS published;
ALTER TABLE notification_schema.outbox_events DROP COLUMN IF EXISTS published_at;
ALTER TABLE notification_schema.outbox_events DROP COLUMN IF EXISTS retry_count;
DROP INDEX IF EXISTS notification_schema.idx_notification_outbox_unpublished;

-- settlement_schema
ALTER TABLE settlement_schema.outbox_events DROP COLUMN IF EXISTS published;
ALTER TABLE settlement_schema.outbox_events DROP COLUMN IF EXISTS published_at;
ALTER TABLE settlement_schema.outbox_events DROP COLUMN IF EXISTS retry_count;
DROP INDEX IF EXISTS settlement_schema.idx_settlement_outbox_unpublished;
