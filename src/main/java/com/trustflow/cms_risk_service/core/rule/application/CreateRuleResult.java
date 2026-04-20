package com.trustflow.cms_risk_service.core.rule.application;

import java.time.Instant;
import java.util.UUID;

public record CreateRuleResult(
        UUID id,
        Instant savedAt
) {
}
