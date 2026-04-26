package com.trustflow.cms_risk_service.infrastructure.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringTakePayloadMapper {
    private final ObjectMapper objectMapper;

    public MonitoringTakePayload map(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            return new MonitoringTakePayload(
                    root.path("integrationId").asLong(),
                    root.path("riskobjectId").asText(null),
                    root.path("data"),
                    root.path("mappingRules")
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to parse monitoring take response body", exception);
        }
    }
}
