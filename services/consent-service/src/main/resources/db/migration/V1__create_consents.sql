CREATE TABLE consents (
    id UUID PRIMARY KEY,
    principal_id VARCHAR(100) NOT NULL,
    resource_owner_id VARCHAR(100) NOT NULL,
    purpose VARCHAR(100) NOT NULL,
    scopes VARCHAR(1000) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL,
    CONSTRAINT consents_status_check CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED'))
);
CREATE INDEX idx_consents_principal ON consents (principal_id);
CREATE INDEX idx_consents_owner_status ON consents (resource_owner_id, status);
