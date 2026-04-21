package com.trustflow.cms_risk_service.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rules-history.cleanup")
public record RuleHistoryCleanupProperties(
        long retentionDays,
        String cron
) {
}
