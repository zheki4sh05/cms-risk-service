package com.trustflow.cms_risk_service.core.rule.application;

import com.trustflow.cms_risk_service.core.rule.domain.RuleAction;
import com.trustflow.cms_risk_service.core.rule.domain.RulePriority;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RuleDetailsResult(
        UUID id,
        UUID companyId,
        String name,
        String condition,
        UUID categoryId,
        UUID riskObjectId,
        RulePriority priority,
        UUID responsibleUserId,
        List<RuleAction> actions,
        boolean enabled,
        String mechanismScriptName,
        String mechanismScriptContent,
        UUID createdByUserId,
        Instant savedAt
) {
}
