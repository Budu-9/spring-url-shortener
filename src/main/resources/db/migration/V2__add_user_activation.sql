ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN verification_token VARCHAR(64);
ALTER TABLE users ADD COLUMN token_expires_at TIMESTAMPTZ;

CREATE INDEX idx_users_verification_token ON users (verification_token) WHERE verification_token IS NOT NULL;