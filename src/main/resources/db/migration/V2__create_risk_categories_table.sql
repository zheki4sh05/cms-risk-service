CREATE TABLE risk_categories (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL
);

INSERT INTO risk_categories (id, company_id, name)
SELECT DISTINCT
    r.category_id,
    r.company_id,
    CONCAT('Category ', r.category_id::TEXT)
FROM rules r
ON CONFLICT (id) DO NOTHING;

CREATE UNIQUE INDEX ux_risk_categories_company_lower_name
    ON risk_categories (company_id, LOWER(name));

ALTER TABLE rules
    ADD CONSTRAINT fk_rules_category_id
        FOREIGN KEY (category_id) REFERENCES risk_categories (id);
