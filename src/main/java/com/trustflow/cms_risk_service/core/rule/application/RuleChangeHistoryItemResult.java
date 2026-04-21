package com.trustflow.cms_risk_service.core.rule.application;

import java.time.Instant;
import java.util.UUID;

public record RuleChangeHistoryItemResult(
        UUID id,
        UUID ruleId,
        Instant changedAt,
        String ruleName,
        String description,
        String authorName
) {
}
