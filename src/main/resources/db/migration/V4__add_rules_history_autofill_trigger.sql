CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE OR REPLACE FUNCTION fill_rules_history_from_rules_changes()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO rules_history (
        id,
        company_id,
        rule_id,
        rule_name,
        description,
        author_id,
        changed_at
    )
    VALUES (
        gen_random_uuid(),
        NEW.company_id,
        NEW.id,
        NEW.name,
        CASE
            WHEN TG_OP = 'INSERT' THEN 'Rule created automatically'
            ELSE 'Rule updated automatically'
        END,
        NEW.created_by_user_id,
        CURRENT_TIMESTAMP
    );

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_rules_fill_history ON rules;

CREATE TRIGGER trg_rules_fill_history
AFTER INSERT OR UPDATE ON rules
FOR EACH ROW
EXECUTE FUNCTION fill_rules_history_from_rules_changes();
