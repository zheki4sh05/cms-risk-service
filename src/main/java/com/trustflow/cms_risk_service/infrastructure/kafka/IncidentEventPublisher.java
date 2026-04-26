package com.trustflow.cms_risk_service.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
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
            List<RuleResultMessage> rules
    ) {
        try {
            CreateIncidentMessage message = new CreateIncidentMessage(
                    companyId,
                    integrationId,
                    riskObjectId,
                    rules
            );
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(kafkaTopicProperties.incident_topic(), payload);
            log.info("Published aggregated incident message for riskObjectId={} with rules={}", riskObjectId, rules.size());
        } catch (Exception exception) {
            log.warn("Failed to publish aggregated incident message for riskObjectId={}", riskObjectId, exception);
        }
    }

    public record RuleResultMessage(
            UUID rulesId,
            String rulePriority,
            String result,
            boolean found,
            Map<String, Object> details
    ) {
    }

    private record CreateIncidentMessage(
            UUID companyId,
            long integrationId,
            String riskObjectId,
            List<RuleResultMessage> rules
    ) {
    }
}
