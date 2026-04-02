-- V5: 회원 도메인 테이블

CREATE TABLE member_schema.members (
    id                          UUID PRIMARY KEY,
    email                       VARCHAR(255) NOT NULL UNIQUE,
    password                    VARCHAR(255) NOT NULL,
    name                        VARCHAR(50) NOT NULL,
    phone                       VARCHAR(20) NOT NULL,
    grade                       VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    status                      VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    accumulated_purchase_amount BIGINT NOT NULL DEFAULT 0,
    registered_at               TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_members_email ON member_schema.members (email);
