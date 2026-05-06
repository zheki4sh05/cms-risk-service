package com.trustflow.cms_risk_service.web.rule.dto;

public record RiskObjectResponse(
        String id,
        String uuid,
        String code,
        String name,
        String status,
        String updatedAt,
        String definition
) {
}
