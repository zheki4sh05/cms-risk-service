package com.trustflow.cms_risk_service.web.rule.dto;

import java.util.List;
import java.util.UUID;

public record RuleChangeHistoryDetailsResponse(
        UUID id,
        UUID companyId,
        UUID ruleId,
        String ruleName,
        String description,
        UUID authorId,
        String authorName,
        String condition,
        UUID categoryId,
        UUID riskObjectId,
        String priority,
        UUID responsibleUserId,
        List<String> actions,
        Boolean enabled,
        String mechanismScriptName,
        String mechanismScriptContent,
        UUID createdByUserId,
        String savedAt,
        String changedAt
) {
}
