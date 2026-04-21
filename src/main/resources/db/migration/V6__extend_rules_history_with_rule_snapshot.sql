ALTER TABLE rules_history
    ADD COLUMN condition_expression TEXT,
    ADD COLUMN category_id UUID,
    ADD COLUMN risk_object_id UUID,
    ADD COLUMN priority VARCHAR(16),
    ADD COLUMN responsible_user_id UUID,
    ADD COLUMN actions TEXT,
    ADD COLUMN enabled BOOLEAN,
    ADD COLUMN mechanism_script_name VARCHAR(255),
    ADD COLUMN mechanism_script_content TEXT,
    ADD COLUMN created_by_user_id UUID,
    ADD COLUMN saved_at TIMESTAMPTZ;
