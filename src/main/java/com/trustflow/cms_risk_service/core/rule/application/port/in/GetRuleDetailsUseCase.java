package com.trustflow.cms_risk_service.core.rule.application.port.in;

import com.trustflow.cms_risk_service.core.rule.application.RuleDetailsResult;

import java.util.UUID;

public interface GetRuleDetailsUseCase {
    RuleDetailsResult getRuleDetails(UUID ruleId);
}
