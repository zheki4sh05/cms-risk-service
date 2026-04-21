CREATE TABLE rules_history (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    rule_id UUID REFERENCES rules(id),
    rule_name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    author_id UUID,
    changed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_rules_history_company_changed_at
    ON rules_history(company_id, changed_at DESC);
