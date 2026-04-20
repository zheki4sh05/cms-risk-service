package com.trustflow.cms_risk_service.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtConfigProperties(
        String publicKey
) {
}
