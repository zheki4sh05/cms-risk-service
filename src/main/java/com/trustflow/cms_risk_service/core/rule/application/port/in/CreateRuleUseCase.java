package com.trustflow.cms_risk_service.core.rule.application.port.in;

import com.trustflow.cms_risk_service.core.rule.application.CreateRuleCommand;
import com.trustflow.cms_risk_service.core.rule.application.CreateRuleResult;

public interface CreateRuleUseCase {
    CreateRuleResult createRule(CreateRuleCommand command);
}
