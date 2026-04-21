package com.trustflow.cms_risk_service.core.rule.application;

import com.trustflow.cms_risk_service.core.rule.domain.RuleAction;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RuleHistoryWriteCommand(
        UUID id,
        UUID companyId,
        UUID ruleId,
        String ruleName,
        String description,
        UUID authorId,
        String condition,
        UUID categoryId,
        UUID riskObjectId,
        String priority,
        UUID responsibleUserId,
        List<RuleAction> actions,
        boolean enabled,
        String mechanismScriptName,
        String mechanismScriptContent,
        UUID createdByUserId,
        Instant savedAt,
        Instant changedAt
) {
}
