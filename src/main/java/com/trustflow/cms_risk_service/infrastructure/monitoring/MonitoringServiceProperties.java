package com.trustflow.cms_risk_service.infrastructure.monitoring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.monitoring-service")
public record MonitoringServiceProperties(
        String baseUrl
) {
}
