package com.trustflow.cms_risk_service.core.rule.application.port.in;

import com.trustflow.cms_risk_service.core.rule.application.UpdateRuleCommand;
import com.trustflow.cms_risk_service.core.rule.application.UpdateRuleResult;

public interface UpdateRuleUseCase {
    UpdateRuleResult updateRule(UpdateRuleCommand command);
}
