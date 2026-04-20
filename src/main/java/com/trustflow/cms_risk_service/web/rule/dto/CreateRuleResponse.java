package com.trustflow.cms_risk_service.web.rule.dto;

import java.util.UUID;

public record CreateRuleResponse(
        UUID id,
        String savedAt
) {
}
