ALTER TABLE rules
    ADD COLUMN success_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN triggers_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN failed_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN last_date_invocation TIMESTAMPTZ,
    ADD COLUMN last_date_trigger DATE;
