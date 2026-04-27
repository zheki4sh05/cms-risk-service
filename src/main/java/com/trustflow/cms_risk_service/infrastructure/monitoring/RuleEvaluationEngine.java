package com.trustflow.cms_risk_service.infrastructure.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustflow.cms_risk_service.infrastructure.kafka.IncidentEventPublisher;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.RuleJpaEntity;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.RuleJpaRepository;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.VerificationResultJpaEntity;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.VerificationResultJpaRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
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
    private static final int THREAD_POOL_SIZE = 4;
    private static final String PAYLOAD_PREFIX = "payload.";
    private static final String SCRIPT_RESULT_SUCCESS = "success";
    private static final String SCRIPT_RESULT_FAILED = "failed";
    private static final String ERROR_KEY = "error";
    private static final String REASON_KEY = "reason";
    private static final String SCRIPT_EXECUTION_FAILED_TEXT = "Script execution failed";

    private static final Pattern UUID_PATTERN =
            Pattern.compile("([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})");

    private final RuleJpaRepository ruleJpaRepository;
    private final ScriptEngineEvaluator scriptEngineEvaluator;
    private final ObjectMapper objectMapper;
    private final IncidentEventPublisher incidentEventPublisher;
    private final VerificationResultJpaRepository verificationResultJpaRepository;
    private final RuleExecutionStatsService ruleExecutionStatsService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    public void submitForProcessing(MonitoringTakePayload payload) {
        executorService.submit(() -> process(payload));
    }

    private void process(MonitoringTakePayload payload) {
        UUID riskObjectId = parseRiskObjectId(payload.riskobjectId());
        if (riskObjectId == null) {
            log.warn("Skip rule evaluation: invalid riskobjectId={}", payload.riskobjectId());
            return;
        }
        String documentId = extractDocumentId(payload.data(), payload.mappingRules());

        List<RuleJpaEntity> risks = ruleJpaRepository.findAllByRiskObjectIdOrderBySavedAtDesc(riskObjectId);
        if (risks.isEmpty()) {
            log.info("No risks found for riskobjectId={}", payload.riskobjectId());
            return;
        }

        Map<String, Object> scriptContext = buildScriptContext(payload);
        List<IncidentEventPublisher.RuleResultMessage> aggregatedRuleResults = new ArrayList<>();
        for (RuleJpaEntity risk : risks) {
            evaluateRule(risk, payload.riskobjectId(), scriptContext)
                    .ifPresent(aggregatedRuleResults::add);
        }

        boolean hasDiscrepancies = aggregatedRuleResults.stream()
                .anyMatch(IncidentEventPublisher.RuleResultMessage::found);
        if (hasDiscrepancies) {
            incidentEventPublisher.publishCreateIncident(
                    risks.get(0).getCompanyId(),
                    payload.integrationId(),
                    payload.riskobjectId(),
                    documentId,
                    aggregatedRuleResults
            );
            return;
        }

        saveVerificationResult(risks.get(0).getCompanyId(), payload, documentId);
    }

    private void saveVerificationResult(UUID companyId, MonitoringTakePayload payload, String documentId) {
        VerificationResultJpaEntity verificationResult = new VerificationResultJpaEntity();
        verificationResult.setId(UUID.randomUUID());
        verificationResult.setCompanyId(companyId);
        verificationResult.setIntegrationId(payload.integrationId());
        verificationResult.setRiskObjectId(payload.riskobjectId());
        verificationResult.setDocumentId(documentId);
        verificationResult.setData(payload.data());
        verificationResultJpaRepository.save(verificationResult);
        log.info(
                "Saved verification_result for riskObjectId={} without discrepancies. documentId={}",
                payload.riskobjectId(),
                documentId
        );
    }

    private String extractDocumentId(JsonNode data, JsonNode mappingRules) {
        if (mappingRules == null || !mappingRules.isArray()) {
            return null;
        }
        for (JsonNode rule : mappingRules) {
            String to = rule.path("to").asText(null);
            String from = rule.path("from").asText(null);
            if (to == null || from == null) {
                continue;
            }
            if (!"id".equalsIgnoreCase(to.trim())) {
                continue;
            }
            JsonNode value = resolveValueFromData(data, from);
            return toDocumentId(value);
        }
        return null;
    }

    private String toDocumentId(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isTextual()) {
            String text = value.asText();
            return text == null || text.isBlank() ? null : text;
        }
        if (value.isNumber() || value.isBoolean()) {
            return value.asText();
        }
        String serialized = value.toString();
        return serialized.isBlank() ? null : serialized;
    }

    private Map<String, Object> buildScriptContext(MonitoringTakePayload payload) {
        Map<String, JsonNode> executionContext = buildExecutionContext(payload.data(), payload.mappingRules());
        return toScriptContext(executionContext);
    }

    private java.util.Optional<IncidentEventPublisher.RuleResultMessage> evaluateRule(
            RuleJpaEntity risk,
            String riskObjectId,
            Map<String, Object> scriptContext
    ) {
        String script = risk.getMechanismScriptContent();
        if (script == null || script.isBlank()) {
            log.info("Skip script execution for riskId={} because mechanism_script_content is empty", risk.getId());
            return java.util.Optional.empty();
        }

        try {
            Object rawResult = scriptEngineEvaluator.evaluate(script, scriptContext);
            ScriptExecutionResult executionResult = mapExecutionResult(rawResult);
            log.info(
                    "Groovy script execution result. riskId={} riskobjectId={} context={} result={}",
                    risk.getId(),
                    riskObjectId,
                    scriptContext,
                    executionResult
            );
            registerExecutionStats(risk.getId(), executionResult);
            return java.util.Optional.of(toRuleResultMessage(risk, executionResult));
        } catch (Exception exception) {
            log.warn(
                    "Groovy script execution failed. riskId={} riskobjectId={} context={}",
                    risk.getId(),
                    riskObjectId,
                    scriptContext,
                    exception
            );
            ruleExecutionStatsService.registerFailure(risk.getId());
            return java.util.Optional.of(buildFailedRuleResult(risk, exception));
        }
    }

    private void registerExecutionStats(UUID ruleId, ScriptExecutionResult executionResult) {
        if (SCRIPT_RESULT_FAILED.equalsIgnoreCase(executionResult.result())) {
            ruleExecutionStatsService.registerFailure(ruleId);
            return;
        }
        if (executionResult.found()) {
            ruleExecutionStatsService.registerTrigger(ruleId);
            return;
        }
        ruleExecutionStatsService.registerSuccess(ruleId);
    }

    private IncidentEventPublisher.RuleResultMessage toRuleResultMessage(RuleJpaEntity risk, ScriptExecutionResult executionResult) {
        return new IncidentEventPublisher.RuleResultMessage(
                risk.getId(),
                risk.getPriority(),
                risk.getResponsibleUserId(),
                executionResult.result(),
                executionResult.found(),
                executionResult.details()
        );
    }

    private IncidentEventPublisher.RuleResultMessage buildFailedRuleResult(RuleJpaEntity risk, Exception exception) {
        Map<String, Object> errorDetails = new LinkedHashMap<>();
        errorDetails.put(ERROR_KEY, SCRIPT_EXECUTION_FAILED_TEXT);
        errorDetails.put(REASON_KEY, exception.getMessage());
        return new IncidentEventPublisher.RuleResultMessage(
                risk.getId(),
                risk.getPriority(),
                risk.getResponsibleUserId(),
                SCRIPT_RESULT_FAILED,
                false,
                errorDetails
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
            status = node.path("success").asBoolean(false) ? SCRIPT_RESULT_SUCCESS : SCRIPT_RESULT_FAILED;
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

        if (from != null && from.startsWith(PAYLOAD_PREFIX)) {
            JsonNode byPayloadPath = readByPath(data, from.substring(PAYLOAD_PREFIX.length()));
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
