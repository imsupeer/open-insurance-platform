CREATE TABLE policies (
    id UUID PRIMARY KEY,
    policy_number VARCHAR(50) NOT NULL UNIQUE,
    owner_id VARCHAR(100) NOT NULL,
    product VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT policies_status_check CHECK (status IN ('ACTIVE', 'CANCELLED'))
);
CREATE INDEX idx_policies_owner ON policies (owner_id);

INSERT INTO policies (id, policy_number, owner_id, product, status)
VALUES ('11111111-1111-1111-1111-111111111111', 'POL-SYN-001', 'customer-001', 'auto', 'ACTIVE');
