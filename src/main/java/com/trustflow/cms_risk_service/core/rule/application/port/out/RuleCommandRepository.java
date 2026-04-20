package com.trustflow.cms_risk_service.core.rule.application.port.out;

import com.trustflow.cms_risk_service.core.rule.domain.Rule;

public interface RuleCommandRepository {
    Rule save(Rule rule);
}
