package com.trustflow.cms_risk_service.infrastructure.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.OutbooxMonitoringJpaEntity;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.OutbooxMonitoringJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskTopicListener {
    private final KafkaTopicProperties kafkaTopicProperties;
    private final OutbooxMonitoringJpaRepository outbooxMonitoringJpaRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.risk-topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onMessage(String message) {
        log.info("Received message from topic {}: {}", kafkaTopicProperties.riskTopic(), message);

        try {
            JsonNode parsedMessage = objectMapper.readTree(message);

            OutbooxMonitoringJpaEntity entity = new OutbooxMonitoringJpaEntity();
            entity.setId(UUID.randomUUID());
            entity.setData(parsedMessage);
            outbooxMonitoringJpaRepository.save(entity);

            log.info("Saved kafka message to outboox_monitoring with id={}", entity.getId());
        } catch (Exception exception) {
            log.warn("Failed to save kafka message to outboox_monitoring", exception);
        }
    }
}
