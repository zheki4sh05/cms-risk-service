package com.trustflow.cms_risk_service.infrastructure.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth-service")
public record AuthServiceProperties(
        String baseUrl
) {
}
