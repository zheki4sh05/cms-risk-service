package com.trustflow.cms_risk_service.infrastructure.monitoring;

import com.fasterxml.jackson.databind.JsonNode;

public record MonitoringTakePayload(
        long integrationId,
        String riskobjectId,
        JsonNode data,
        JsonNode mappingRules
) {
}
