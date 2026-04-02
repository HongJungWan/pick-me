-- V12: DLT (Dead Letter) 모니터링 테이블

CREATE TABLE dead_letter_events (
    id              BIGSERIAL PRIMARY KEY,
    event_id        UUID NOT NULL,
    event_type      VARCHAR(200) NOT NULL,
    original_topic  VARCHAR(100) NOT NULL,
    payload         TEXT NOT NULL,
    error_message   TEXT,
    status          VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    retry_count     INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    retried_at      TIMESTAMP
);

CREATE INDEX idx_dlt_status ON dead_letter_events (status);
CREATE INDEX idx_dlt_event_id ON dead_letter_events (event_id);
