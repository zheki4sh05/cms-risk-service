package com.trustflow.cms_risk_service.infrastructure.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustflow.cms_risk_service.infrastructure.kafka.IncidentEventPublisher;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.RuleJpaEntity;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.RuleJpaRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleEvaluationEngine {
    private static final Pattern UUID_PATTERN =
            Pattern.compile("([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})");

    private final RuleJpaRepository ruleJpaRepository;
    private final ScriptEngineEvaluator scriptEngineEvaluator;
    private final ObjectMapper objectMapper;
    private final IncidentEventPublisher incidentEventPublisher;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    public void submitForProcessing(MonitoringTakePayload payload) {
        executorService.submit(() -> process(payload));
    }

    private void process(MonitoringTakePayload payload) {
        UUID riskObjectId = parseRiskObjectId(payload.riskobjectId());
        if (riskObjectId == null) {
            log.warn("Skip rule evaluation: invalid riskobjectId={}", payload.riskobjectId());
            return;
        }

        List<RuleJpaEntity> risks = ruleJpaRepository.findAllByRiskObjectIdOrderBySavedAtDesc(riskObjectId);
        if (risks.isEmpty()) {
            log.info("No risks found for riskobjectId={}", payload.riskobjectId());
            return;
        }

        Map<String, JsonNode> executionContext = buildExecutionContext(payload.data(), payload.mappingRules());
        Map<String, Object> scriptContext = toScriptContext(executionContext);
        for (RuleJpaEntity risk : risks) {
            String script = risk.getMechanismScriptContent();
            if (script == null || script.isBlank()) {
                log.info("Skip script execution for riskId={} because mechanism_script_content is empty", risk.getId());
                continue;
            }

            try {
                Object result = scriptEngineEvaluator.evaluate(script, scriptContext);
                ScriptExecutionResult executionResult = mapExecutionResult(result);
                log.info(
                        "Groovy script execution result. riskId={} riskobjectId={} context={} result={}",
                        risk.getId(),
                        payload.riskobjectId(),
                        scriptContext,
                        executionResult
                );
                processExecutionResult(payload, risk, executionResult);
            } catch (Exception exception) {
                log.warn(
                        "Groovy script execution failed. riskId={} riskobjectId={} context={}",
                        risk.getId(),
                        payload.riskobjectId(),
                        scriptContext,
                        exception
                );
            }
        }
    }

    private Map<String, JsonNode> buildExecutionContext(JsonNode data, JsonNode mappingRules) {
        Map<String, JsonNode> context = new LinkedHashMap<>();
        if (mappingRules == null || !mappingRules.isArray()) {
            return context;
        }

        for (JsonNode rule : mappingRules) {
            String from = rule.path("from").asText(null);
            String to = rule.path("to").asText(null);
            if (from == null || from.isBlank()) {
                continue;
            }

            JsonNode value = resolveValueFromData(data, from, to);
            context.put(from, value);
        }
        return context;
    }

    private Map<String, Object> toScriptContext(Map<String, JsonNode> executionContext) {
        Map<String, Object> scriptContext = new LinkedHashMap<>();
        executionContext.forEach((key, value) -> scriptContext.put(key, objectMapper.convertValue(value, Object.class)));
        return scriptContext;
    }

    private ScriptExecutionResult mapExecutionResult(Object rawResult) {
        JsonNode node = objectMapper.valueToTree(rawResult);
        return new ScriptExecutionResult(
                node.path("result").asText("failed"),
                node.path("details").asText(""),
                node.path("found").asBoolean(false)
        );
    }

    private void processExecutionResult(MonitoringTakePayload payload, RuleJpaEntity risk, ScriptExecutionResult executionResult) {
        if (!"success".equalsIgnoreCase(executionResult.result())) {
            return;
        }
        if (!executionResult.found()) {
            return;
        }
        if (risk.getActions() == null || risk.getActions().stream().noneMatch("createIncident"::equalsIgnoreCase)) {
            return;
        }

        incidentEventPublisher.publishCreateIncident(
                risk.getCompanyId(),
                payload.integrationId(),
                payload.riskobjectId(),
                risk.getId(),
                risk.getPriority()
        );
    }

    private JsonNode resolveValueFromData(JsonNode data, String from, String to) {
        JsonNode byTo = readByPath(data, to);
        if (byTo != null) {
            return byTo;
        }

        if (to != null && to.startsWith("payload.")) {
            JsonNode byPayloadPath = readByPath(data, to.substring("payload.".length()));
            if (byPayloadPath != null) {
                return byPayloadPath;
            }
        }

        JsonNode byFrom = readByPath(data, from);
        if (byFrom != null) {
            return byFrom;
        }

        return null;
    }

    private JsonNode readByPath(JsonNode source, String path) {
        if (source == null || path == null || path.isBlank()) {
            return null;
        }

        JsonNode cursor = source;
        for (String part : path.split("\\.")) {
            if (cursor == null || cursor.isMissingNode() || cursor.isNull()) {
                return null;
            }
            cursor = cursor.path(part);
        }

        if (cursor == null || cursor.isMissingNode() || cursor.isNull()) {
            return null;
        }
        return cursor;
    }

    private UUID parseRiskObjectId(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(rawValue);
        } catch (IllegalArgumentException ignored) {
            Matcher matcher = UUID_PATTERN.matcher(rawValue);
            if (matcher.find()) {
                return UUID.fromString(matcher.group(1));
            }
            return null;
        }
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
    }

    private record ScriptExecutionResult(
            String result,
            String details,
            boolean found
    ) {
    }
}
