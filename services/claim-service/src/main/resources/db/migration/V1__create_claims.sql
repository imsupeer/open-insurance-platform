CREATE TABLE claims (
    id UUID PRIMARY KEY,
    principal_id VARCHAR(100) NOT NULL,
    policy_id UUID NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    description VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT claims_status_check CHECK (status IN ('OPEN')),
    CONSTRAINT claims_principal_idempotency_uq UNIQUE (principal_id, idempotency_key)
);
CREATE INDEX idx_claims_principal ON claims (principal_id);
