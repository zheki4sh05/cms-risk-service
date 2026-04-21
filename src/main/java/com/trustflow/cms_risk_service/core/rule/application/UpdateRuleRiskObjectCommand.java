package com.trustflow.cms_risk_service.core.rule.application;

import java.util.UUID;

public record UpdateRuleRiskObjectCommand(
        UUID id,
        UUID riskObjectId
) {
}
