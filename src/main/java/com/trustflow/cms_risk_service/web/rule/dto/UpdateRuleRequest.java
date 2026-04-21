package com.trustflow.cms_risk_service.web.rule.dto;

import com.trustflow.cms_risk_service.core.rule.domain.RuleAction;
import com.trustflow.cms_risk_service.core.rule.domain.RulePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record UpdateRuleRequest(
        @NotBlank(message = "description is required")
        String description,
        @NotBlank(message = "name is required")
        String name,
        @NotBlank(message = "condition is required")
        String condition,
        @NotNull(message = "categoryId is required")
        UUID categoryId,
        UUID riskObjectId,
        @NotNull(message = "priority is required")
        RulePriority priority,
        UUID responsibleUserId,
        @NotEmpty(message = "actions must contain at least one action")
        List<RuleAction> actions,
        @NotNull(message = "enabled is required")
        Boolean enabled,
        String mechanismScriptName,
        String mechanismScriptContent
) {
}
