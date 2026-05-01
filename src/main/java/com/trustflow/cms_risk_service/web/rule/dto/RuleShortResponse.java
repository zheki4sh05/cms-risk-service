package com.trustflow.cms_risk_service.web.rule.dto;

import java.util.UUID;

public record RuleShortResponse(
        UUID id,
        UUID companyId,
        String name,
        String condition,
        UUID categoryId,
        String priority,
        UUID responsibleUserId
) {
}
