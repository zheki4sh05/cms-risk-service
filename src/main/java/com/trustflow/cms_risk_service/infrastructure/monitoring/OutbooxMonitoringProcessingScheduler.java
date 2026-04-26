package com.trustflow.cms_risk_service.infrastructure.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.OutbooxMonitoringJpaEntity;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.OutbooxMonitoringJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutbooxMonitoringProcessingScheduler {
    private final OutbooxMonitoringJpaRepository outbooxMonitoringJpaRepository;
    private final RestClient monitoringServiceRestClient;
    private final MonitoringTakePayloadMapper monitoringTakePayloadMapper;
    private final RuleEvaluationEngine ruleEvaluationEngine;

    @Scheduled(fixedDelayString = "${app.monitoring-processing.fixed-delay-ms:5000}")
    @Transactional
    public void processRows() {
        List<OutbooxMonitoringJpaEntity> rows = outbooxMonitoringJpaRepository.findTop50ByOrderByIdAsc();
        for (OutbooxMonitoringJpaEntity row : rows) {
            processRow(row);
        }
    }

    private void processRow(OutbooxMonitoringJpaEntity row) {
        log.info("Processing outboox_monitoring row id={} data={}", row.getId(), row.getData());

        String monitoringEntityId = extractMonitoringEntityId(row);
        if (monitoringEntityId == null) {
            return;
        }

        MonitoringTakeResponse response;
        try {
            response = monitoringServiceRestClient.put()
                    .uri("/api/monitoring-results/{id}/take", monitoringEntityId)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange((request, clientResponse) -> {
                        HttpStatusCode statusCode = clientResponse.getStatusCode();
                        String body = readResponseBody(clientResponse);
                        return new MonitoringTakeResponse(statusCode.value(), body);
                    });
        } catch (RestClientException exception) {
            log.warn("Failed to call monitoring service for row id={} monitoringEntity={}", row.getId(), monitoringEntityId, exception);
            return;
        }

        log.info(
                "Monitoring service response for row id={} monitoringEntity={} status={} body={}",
                row.getId(),
                monitoringEntityId,
                response.statusCode(),
                response.body()
        );

        if (response.statusCode() == 200) {
            try {
                MonitoringTakePayload payload = monitoringTakePayloadMapper.map(response.body());
                ruleEvaluationEngine.submitForProcessing(payload);
            } catch (Exception exception) {
                log.warn(
                        "Failed to map monitoring response for row id={} monitoringEntity={}",
                        row.getId(),
                        monitoringEntityId,
                        exception
                );
            }
            outbooxMonitoringJpaRepository.deleteById(row.getId());
            log.info("Deleted row id={} after successful processing", row.getId());
            return;
        }

        if (response.statusCode() != 200) {
            outbooxMonitoringJpaRepository.deleteById(row.getId());
            log.warn("Deleted row id={} because monitoring service returned non-200 status={}", row.getId(), response.statusCode());
        }
    }

    private String extractMonitoringEntityId(OutbooxMonitoringJpaEntity row) {
        try {
            JsonNode root = row.getData();
            JsonNode monitoringEntityNode = root.path("monitoring_entity");
            if (monitoringEntityNode.isMissingNode() || monitoringEntityNode.isNull()) {
                log.warn("Skip row id={}: missing or invalid field monitoring_entity", row.getId());
                outbooxMonitoringJpaRepository.deleteById(row.getId());
                log.warn("Deleted row id={} due to invalid payload", row.getId());
                return null;
            }

            String monitoringEntity = monitoringEntityNode.asText();
            if (monitoringEntity == null || monitoringEntity.isBlank()) {
                log.warn("Skip row id={}: empty field monitoring_entity", row.getId());
                outbooxMonitoringJpaRepository.deleteById(row.getId());
                log.warn("Deleted row id={} due to invalid payload", row.getId());
                return null;
            }
            return monitoringEntity;
        } catch (Exception exception) {
            log.warn("Skip row id={} due to invalid payload", row.getId(), exception);
            outbooxMonitoringJpaRepository.deleteById(row.getId());
            log.warn("Deleted row id={} due to invalid payload", row.getId());
            return null;
        }
    }

    private String readResponseBody(ClientHttpResponse clientResponse) {
        try {
            return new String(clientResponse.getBody().readAllBytes());
        } catch (IOException exception) {
            log.warn("Could not read monitoring service response body", exception);
            return "";
        }
    }

    private record MonitoringTakeResponse(int statusCode, String body) {
    }
}
