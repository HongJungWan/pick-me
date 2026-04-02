-- V13: orders 테이블 월별 Range Partitioning (PostgreSQL 전용)
-- 기존 orders 테이블을 파티션 테이블로 재생성
-- 주의: 이 마이그레이션은 PostgreSQL에서만 실행 (H2 비호환)

-- 1. 기존 테이블 백업 및 삭제
ALTER TABLE order_schema.order_lines DROP CONSTRAINT IF EXISTS order_lines_order_id_fkey;

CREATE TABLE order_schema.orders_backup AS SELECT * FROM order_schema.orders;
CREATE TABLE order_schema.order_lines_backup AS SELECT * FROM order_schema.order_lines;

DROP TABLE IF EXISTS order_schema.order_lines;
DROP TABLE IF EXISTS order_schema.orders;

-- 2. 파티션 테이블 생성
CREATE TABLE order_schema.orders (
    id              UUID NOT NULL,
    orderer_id      UUID NOT NULL,
    order_status    VARCHAR(20) NOT NULL DEFAULT 'PLACED',
    total_amount    BIGINT NOT NULL DEFAULT 0,
    receiver_name   VARCHAR(50) NOT NULL,
    receiver_phone  VARCHAR(20) NOT NULL,
    zip_code        VARCHAR(10) NOT NULL,
    road_address    VARCHAR(200) NOT NULL,
    address_detail  VARCHAR(200),
    ordered_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, ordered_at)
) PARTITION BY RANGE (ordered_at);

-- 3. 2026년 월별 파티션 생성
CREATE TABLE order_schema.orders_2026_01 PARTITION OF order_schema.orders
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE order_schema.orders_2026_02 PARTITION OF order_schema.orders
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE order_schema.orders_2026_03 PARTITION OF order_schema.orders
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE order_schema.orders_2026_04 PARTITION OF order_schema.orders
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE order_schema.orders_2026_05 PARTITION OF order_schema.orders
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
CREATE TABLE order_schema.orders_2026_06 PARTITION OF order_schema.orders
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
CREATE TABLE order_schema.orders_2026_07 PARTITION OF order_schema.orders
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
CREATE TABLE order_schema.orders_2026_08 PARTITION OF order_schema.orders
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');
CREATE TABLE order_schema.orders_2026_09 PARTITION OF order_schema.orders
    FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');
CREATE TABLE order_schema.orders_2026_10 PARTITION OF order_schema.orders
    FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');
CREATE TABLE order_schema.orders_2026_11 PARTITION OF order_schema.orders
    FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');
CREATE TABLE order_schema.orders_2026_12 PARTITION OF order_schema.orders
    FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');

-- 기본 파티션 (범위 밖 데이터 수용)
CREATE TABLE order_schema.orders_default PARTITION OF order_schema.orders DEFAULT;

-- 4. 인덱스 재생성
CREATE INDEX idx_orders_orderer_status ON order_schema.orders (orderer_id, order_status);
CREATE INDEX idx_orders_ordered_at ON order_schema.orders (ordered_at);

-- 5. order_lines 테이블 재생성
CREATE TABLE order_schema.order_lines (
    id              BIGSERIAL PRIMARY KEY,
    order_id        UUID NOT NULL,
    product_id      UUID NOT NULL,
    product_name    VARCHAR(200) NOT NULL,
    quantity        INT NOT NULL,
    unit_price      BIGINT NOT NULL,
    line_total      BIGINT NOT NULL
);
CREATE INDEX idx_order_lines_order ON order_schema.order_lines (order_id);

-- 6. 데이터 복원
INSERT INTO order_schema.orders SELECT * FROM order_schema.orders_backup;
INSERT INTO order_schema.order_lines SELECT * FROM order_schema.order_lines_backup;

-- 7. 백업 테이블 삭제
DROP TABLE IF EXISTS order_schema.orders_backup;
DROP TABLE IF EXISTS order_schema.order_lines_backup;
