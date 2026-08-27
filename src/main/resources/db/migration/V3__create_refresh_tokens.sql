-- V3__create_refresh_tokens.sql
CREATE TABLE refresh_tokens (
                                id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                                token_hash    VARCHAR(64) NOT NULL UNIQUE,
                                user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                expires_at    TIMESTAMPTZ NOT NULL,
                                revoked       BOOLEAN     NOT NULL DEFAULT false,
                                created_at    TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
                                last_used_at  TIMESTAMPTZ,
                                ip_address    VARCHAR(45),
                                device        VARCHAR(255)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);