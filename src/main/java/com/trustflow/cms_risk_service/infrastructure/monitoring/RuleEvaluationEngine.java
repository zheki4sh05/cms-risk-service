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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
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
        List<IncidentEventPublisher.RuleResultMessage> aggregatedRuleResults = new ArrayList<>();
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
                aggregatedRuleResults.add(
                        new IncidentEventPublisher.RuleResultMessage(
                                risk.getId(),
                                risk.getPriority(),
                                executionResult.result(),
                                executionResult.found(),
                                executionResult.details()
                        )
                );
            } catch (Exception exception) {
                log.warn(
                        "Groovy script execution failed. riskId={} riskobjectId={} context={}",
                        risk.getId(),
                        payload.riskobjectId(),
                        scriptContext,
                        exception
                );
                Map<String, Object> errorDetails = new LinkedHashMap<>();
                errorDetails.put("error", "Script execution failed");
                errorDetails.put("reason", exception.getMessage());
                aggregatedRuleResults.add(
                        new IncidentEventPublisher.RuleResultMessage(
                                risk.getId(),
                                risk.getPriority(),
                                "failed",
                                false,
                                errorDetails
                        )
                );
            }
        }
        incidentEventPublisher.publishCreateIncident(
                risks.get(0).getCompanyId(),
                payload.integrationId(),
                payload.riskobjectId(),
                aggregatedRuleResults
        );
    }

    private Map<String, JsonNode> buildExecutionContext(JsonNode data, JsonNode mappingRules) {
        Map<String, JsonNode> context = new LinkedHashMap<>();
        if (mappingRules == null || !mappingRules.isArray()) {
            return context;
        }

        for (JsonNode rule : mappingRules) {
            String from = rule.path("from").asText(null);
            String to = rule.path("to").asText(null);
            if (from == null || from.isBlank() || to == null || to.isBlank()) {
                continue;
            }

            JsonNode value = resolveValueFromData(data, from);
            context.put(to, value);
        }
        return context;
    }

    private Map<String, Object> toScriptContext(Map<String, JsonNode> executionContext) {
        Map<String, Object> params = new LinkedHashMap<>();
        executionContext.forEach((key, value) -> params.put(key, objectMapper.convertValue(value, Object.class)));

        Map<String, Object> scriptContext = new LinkedHashMap<>();
        scriptContext.put("params", params);
        // Keep backward compatibility for scripts that reference variables directly.
        scriptContext.putAll(params);
        return scriptContext;
    }

    private ScriptExecutionResult mapExecutionResult(Object rawResult) {
        JsonNode node = objectMapper.valueToTree(rawResult);
        String status = node.path("result").asText("");
        if (status.isBlank()) {
            status = node.path("success").asBoolean(false) ? "success" : "failed";
        }
        return new ScriptExecutionResult(
                status,
                readDetails(node.path("details")),
                node.path("found").asBoolean(false)
        );
    }

    private Map<String, Object> readDetails(JsonNode detailsNode) {
        if (detailsNode == null || detailsNode.isMissingNode() || detailsNode.isNull()) {
            return Collections.emptyMap();
        }
        if (detailsNode.isObject()) {
            Map<String, Object> rawDetails = objectMapper.convertValue(detailsNode, Map.class);
            return normalizeDetails(rawDetails);
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("value", normalizeValue(objectMapper.convertValue(detailsNode, Object.class)));
        return details;
    }

    private Map<String, Object> normalizeDetails(Map<String, Object> rawDetails) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        rawDetails.forEach((key, value) -> normalized.put(key, normalizeValue(value)));
        return normalized;
    }

    private Object normalizeValue(Object value) {
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (value instanceof Map<?, ?> rawMap) {
            if (looksLikeSerializedGString(rawMap)) {
                return decodeGStringBytes(rawMap);
            }

            Map<String, Object> nested = new LinkedHashMap<>();
            rawMap.forEach((k, v) -> nested.put(String.valueOf(k), normalizeValue(v)));
            return nested;
        }
        if (value instanceof List<?> rawList) {
            List<Object> normalizedList = new ArrayList<>(rawList.size());
            for (Object item : rawList) {
                normalizedList.add(normalizeValue(item));
            }
            return normalizedList;
        }
        return value;
    }

    private boolean looksLikeSerializedGString(Map<?, ?> rawMap) {
        return rawMap.containsKey("bytes")
                && rawMap.containsKey("strings")
                && rawMap.containsKey("values");
    }

    private String decodeGStringBytes(Map<?, ?> rawMap) {
        Object bytesValue = rawMap.get("bytes");
        if (bytesValue instanceof byte[] rawBytes) {
            return new String(rawBytes, StandardCharsets.UTF_8);
        }
        if (!(bytesValue instanceof String base64String) || base64String.isBlank()) {
            return String.valueOf(normalizeValue(bytesValue));
        }
        try {
            return new String(Base64.getDecoder().decode(base64String), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return base64String;
        }
    }

    private JsonNode resolveValueFromData(JsonNode data, String from) {
        JsonNode byFrom = readByPath(data, from);
        if (byFrom != null) {
            return byFrom;
        }

        if (from != null && from.startsWith("payload.")) {
            JsonNode byPayloadPath = readByPath(data, from.substring("payload.".length()));
            if (byPayloadPath != null) {
                return byPayloadPath;
            }
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
            Map<String, Object> details,
            boolean found
    ) {
    }
}
