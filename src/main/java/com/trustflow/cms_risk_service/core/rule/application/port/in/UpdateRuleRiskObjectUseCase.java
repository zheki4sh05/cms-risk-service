package com.trustflow.cms_risk_service.core.rule.application.port.in;

import com.trustflow.cms_risk_service.core.rule.application.UpdateRuleResult;
import com.trustflow.cms_risk_service.core.rule.application.UpdateRuleRiskObjectCommand;

public interface UpdateRuleRiskObjectUseCase {
    UpdateRuleResult updateRuleRiskObject(UpdateRuleRiskObjectCommand command);
}
