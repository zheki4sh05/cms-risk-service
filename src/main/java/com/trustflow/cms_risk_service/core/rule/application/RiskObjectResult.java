package com.trustflow.cms_risk_service.core.rule.application;

import java.time.Instant;

public record RiskObjectResult(
        String id,
        String uuid,
        String code,
        String name,
        String status,
        Instant updatedAt,
        String definition
) {
}
