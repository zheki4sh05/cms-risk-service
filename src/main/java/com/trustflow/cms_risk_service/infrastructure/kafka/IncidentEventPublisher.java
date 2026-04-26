package com.trustflow.cms_risk_service.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicProperties kafkaTopicProperties;
    private final ObjectMapper objectMapper;

    public void publishCreateIncident(
            UUID companyId,
            long integrationId,
            String riskObjectId,
            UUID rulesId,
            String rulePriority
    ) {
        try {
            CreateIncidentMessage message = new CreateIncidentMessage(
                    companyId,
                    integrationId,
                    riskObjectId,
                    rulesId,
                    rulePriority
            );
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(kafkaTopicProperties.incident_topic(), payload);
            log.info("Published createIncident message for ruleId={} riskObjectId={}", rulesId, riskObjectId);
        } catch (Exception exception) {
            log.warn("Failed to publish createIncident message for ruleId={} riskObjectId={}", rulesId, riskObjectId, exception);
        }
    }

    private record CreateIncidentMessage(
            UUID companyId,
            long integrationId,
            String riskObjectId,
            UUID rulesId,
            String rulePriority
    ) {
    }
}
