package com.trustflow.cms_risk_service.infrastructure.riskobject;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.risk-object-service")
public record RiskObjectServiceProperties(
        String baseUrl
) {
}
