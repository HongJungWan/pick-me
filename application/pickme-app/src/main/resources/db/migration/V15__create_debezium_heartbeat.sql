-- V15: Debezium CDC heartbeat 테이블
-- Debezium 커넥터가 주기적으로 UPDATE하여 replication slot의 WAL LSN을 전진시킴
-- 활동이 없는 기간에도 WAL이 무한 축적되는 것을 방지

CREATE TABLE order_schema.debezium_heartbeat (
    id  INT PRIMARY KEY,
    ts  TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO order_schema.debezium_heartbeat (id, ts) VALUES (1, NOW());
