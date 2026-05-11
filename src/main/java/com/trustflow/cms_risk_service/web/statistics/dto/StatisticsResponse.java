package com.trustflow.cms_risk_service.web.statistics.dto;

public record StatisticsResponse(
        long outboxCount,
        long verificationResultCount
) {
}
