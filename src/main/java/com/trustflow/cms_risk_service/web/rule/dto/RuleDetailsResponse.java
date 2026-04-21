package com.trustflow.cms_risk_service.web.rule.dto;

import java.util.List;
import java.util.UUID;

public record RuleDetailsResponse(
        UUID id,
        UUID companyId,
        String name,
        String condition,
        UUID categoryId,
        UUID riskObjectId,
        String priority,
        UUID responsibleUserId,
        List<String> actions,
        boolean enabled,
        String mechanismScriptName,
        String mechanismScriptContent,
        UUID createdByUserId,
        String savedAt
) {
}
