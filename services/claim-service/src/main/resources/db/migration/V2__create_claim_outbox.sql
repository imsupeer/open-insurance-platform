CREATE TABLE claim_outbox (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    aggregate_id UUID NOT NULL,
    topic VARCHAR(200) NOT NULL,
    event_key VARCHAR(200) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    last_error VARCHAR(500),
    CONSTRAINT claim_outbox_status_check CHECK (status IN ('PENDING', 'PUBLISHED'))
);
CREATE INDEX idx_claim_outbox_pending ON claim_outbox (status, available_at, created_at);
