CREATE TABLE verification_result
(
    id             UUID PRIMARY KEY,
    company_id     UUID   NOT NULL,
    integration_id BIGINT NOT NULL,
    risk_object_id TEXT   NOT NULL,
    data           JSONB  NOT NULL
);
