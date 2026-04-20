CREATE TABLE rules (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    condition_expression TEXT NOT NULL,
    category_id UUID NOT NULL,
    risk_object_id UUID,
    priority VARCHAR(16) NOT NULL,
    responsible_user_id UUID,
    actions TEXT NOT NULL,
    enabled BOOLEAN NOT NULL,
    mechanism_script_name VARCHAR(255),
    mechanism_script_content TEXT,
    created_by_user_id UUID NOT NULL,
    saved_at TIMESTAMPTZ NOT NULL
);
