CREATE TABLE processed_claim_events (
    consumer_name VARCHAR(100) NOT NULL,
    event_id UUID NOT NULL,
    claim_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (consumer_name, event_id)
);

CREATE TABLE claim_event_dlt (
    id UUID PRIMARY KEY,
    event_id UUID,
    payload TEXT NOT NULL,
    failure_reason VARCHAR(500) NOT NULL,
    failed_at TIMESTAMPTZ NOT NULL,
    reprocess_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);
