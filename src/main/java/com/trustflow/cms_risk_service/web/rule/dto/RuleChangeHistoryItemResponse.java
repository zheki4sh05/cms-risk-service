package com.trustflow.cms_risk_service.web.rule.dto;

import java.util.UUID;

public record RuleChangeHistoryItemResponse(
        UUID id,
        UUID ruleId,
        String changedAt,
        String ruleName,
        String description,
        String authorName
) {
}
