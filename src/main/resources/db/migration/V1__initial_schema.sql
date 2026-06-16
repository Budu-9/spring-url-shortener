-- V1__initial_schema.sql
-- URL Shortener initial schema

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─── Users ──────────────────────────────────────────────────────────────────
CREATE TABLE users (
                       id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                       email       VARCHAR(255) NOT NULL UNIQUE,
                       password    VARCHAR(255) NOT NULL,
                       role        VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
                       created_at  TIMESTAMPTZ  NOT NULL DEFAULT clock_timestamp(),
                       updated_at  TIMESTAMPTZ  NOT NULL DEFAULT clock_timestamp()
);

-- Note: No need for idx_users_email. PostgreSQL automatically generates an index for UNIQUE columns.

-- ─── URLs ───────────────────────────────────────────────────────────────────
CREATE TABLE urls (
                      id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                      short_code   VARCHAR(30) NOT NULL UNIQUE,
                      long_url     TEXT        NOT NULL,
                      owner_id     UUID        REFERENCES users(id) ON DELETE SET NULL,
                      title        VARCHAR(255),
                      active       BOOLEAN     NOT NULL DEFAULT true,
                      click_count  BIGINT      NOT NULL DEFAULT 0,
                      expires_at   TIMESTAMPTZ,
                      created_at   TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
                      updated_at   TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

-- Removed: Redundant idx_urls_short_code index (UNIQUE constraint handles this automatically)
CREATE INDEX idx_urls_owner_id    ON urls (owner_id);
CREATE INDEX idx_urls_created_at  ON urls (created_at DESC);
-- Partial index for active expirations is excellent choice!
CREATE INDEX idx_urls_expires_at  ON urls (expires_at) WHERE expires_at IS NOT NULL;

-- ─── Clicks ─────────────────────────────────────────────────────────────────
CREATE TABLE clicks (
                        id          BIGSERIAL   PRIMARY KEY,
                        url_id      UUID        NOT NULL REFERENCES urls(id) ON DELETE CASCADE,
                        ip_address  INET,
                        user_agent  TEXT,
                        referer     TEXT,
                        country     VARCHAR(2), -- Changed to 2 for standard ISO 3166-1 alpha-2 codes (e.g., 'US', 'NG')
                        clicked_at  TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

-- Composite index optimized for dashboard analytics queries (Filtering by URL + sorting by time)
CREATE INDEX idx_clicks_url_analytics ON clicks (url_id, clicked_at DESC);

-- ─── Auto-update updated_at trigger ─────────────────────────────────────────
CREATE OR REPLACE FUNCTION trigger_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = clock_timestamp();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_updated_at_users
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

CREATE TRIGGER set_updated_at_urls
    BEFORE UPDATE ON urls
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();