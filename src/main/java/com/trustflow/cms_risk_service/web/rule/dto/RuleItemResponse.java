package com.trustflow.cms_risk_service.web.rule.dto;

import java.util.UUID;

public record RuleItemResponse(
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
