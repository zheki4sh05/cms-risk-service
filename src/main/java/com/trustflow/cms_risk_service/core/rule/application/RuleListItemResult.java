package com.trustflow.cms_risk_service.core.rule.application;

import java.util.UUID;

public record RuleListItemResult(
        UUID id,
        String name,
        String condition,
        String action,
        UUID categoryId,
        String categoryLabel,
        String priority,
        boolean enabled,
        UUID riskObjectId
) {
}
