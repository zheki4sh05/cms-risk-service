package com.trustflow.cms_risk_service.web.rule.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InternalRuleResponse(
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
        Instant savedAt,
        long successCount,
        long triggersCount,
        long failedCount,
        Instant lastDateInvocation,
        LocalDate lastDateTrigger
) {
}
