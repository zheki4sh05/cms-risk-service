package com.trustflow.cms_risk_service.infrastructure.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.time.Instant;
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
    private static final String ARRAY_TOKEN = "[]";

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
                executionResult.details(),
                Instant.now().toString()
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
                errorDetails,
                Instant.now().toString()
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
            applyArrayObjectProjection(context, data, from, to);
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
        JsonNode arrayAwareValue = readArrayAwareByPath(data, from);
        if (arrayAwareValue != null) {
            return arrayAwareValue;
        }

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

    private JsonNode readArrayAwareByPath(JsonNode source, String path) {
        ArrayPathParts parts = splitByArrayMarker(path);
        if (parts == null) {
            return null;
        }

        JsonNode arrayNode = readByPath(source, parts.arrayPath());
        if (arrayNode == null || !arrayNode.isArray()) {
            return null;
        }

        ArrayNode result = objectMapper.createArrayNode();
        for (JsonNode item : arrayNode) {
            if (parts.itemPath() == null || parts.itemPath().isBlank()) {
                result.add(item);
                continue;
            }
            JsonNode resolved = readByPathWithFallback(item, parts.itemPath());
            result.add(resolved == null ? objectMapper.nullNode() : resolved);
        }
        return result;
    }

    private void applyArrayObjectProjection(Map<String, JsonNode> context, JsonNode data, String from, String to) {
        ArrayPathParts sourceParts = splitByArrayMarker(from);
        ArrayPathParts targetParts = splitByArrayMarker(to);
        if (sourceParts == null || targetParts == null) {
            return;
        }
        if (targetParts.rootKey() == null || targetParts.rootKey().isBlank()) {
            return;
        }

        JsonNode sourceArray = readByPath(data, sourceParts.arrayPath());
        if (sourceArray == null || !sourceArray.isArray()) {
            return;
        }

        ArrayNode targetArray;
        JsonNode existing = context.get(targetParts.rootKey());
        if (existing instanceof ArrayNode existingArray) {
            targetArray = existingArray;
        } else {
            targetArray = objectMapper.createArrayNode();
            context.put(targetParts.rootKey(), targetArray);
        }

        int index = 0;
        for (JsonNode sourceItem : sourceArray) {
            while (targetArray.size() <= index) {
                targetArray.add(objectMapper.createObjectNode());
            }

            JsonNode value = sourceParts.itemPath() == null || sourceParts.itemPath().isBlank()
                    ? sourceItem
                    : readByPathWithFallback(sourceItem, sourceParts.itemPath());

            JsonNode safeValue = value == null ? objectMapper.nullNode() : value;
            JsonNode targetItem = targetArray.get(index);
            ObjectNode targetObject;
            if (targetItem instanceof ObjectNode existingObject) {
                targetObject = existingObject;
            } else {
                targetObject = objectMapper.createObjectNode();
                targetArray.set(index, targetObject);
            }
            setPath(targetObject, targetParts.itemPath(), safeValue);
            index++;
        }
    }

    private void setPath(ObjectNode target, String path, JsonNode value) {
        if (path == null || path.isBlank()) {
            if (value != null && value.isObject()) {
                target.setAll((ObjectNode) value);
            }
            return;
        }

        String[] parts = path.split("\\.");
        ObjectNode cursor = target;
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            JsonNode next = cursor.get(part);
            ObjectNode nextObject;
            if (next instanceof ObjectNode existingObject) {
                nextObject = existingObject;
            } else {
                nextObject = objectMapper.createObjectNode();
                cursor.set(part, nextObject);
            }
            cursor = nextObject;
        }
        cursor.set(parts[parts.length - 1], value);
    }

    private ArrayPathParts splitByArrayMarker(String path) {
        if (path == null || path.isBlank() || !path.contains(ARRAY_TOKEN)) {
            return null;
        }

        String[] parts = path.split("\\.");
        List<String> prefix = new ArrayList<>();
        int arrayIndex = -1;
        String arrayKey = null;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.endsWith(ARRAY_TOKEN)) {
                arrayIndex = i;
                arrayKey = part.substring(0, part.length() - ARRAY_TOKEN.length());
                break;
            }
            prefix.add(part);
        }
        if (arrayIndex == -1 || arrayKey == null || arrayKey.isBlank()) {
            return null;
        }

        prefix.add(arrayKey);
        String arrayPath = String.join(".", prefix);
        String itemPath = arrayIndex + 1 < parts.length
                ? String.join(".", java.util.Arrays.copyOfRange(parts, arrayIndex + 1, parts.length))
                : "";
        String rootKey = parts.length > 0
                ? parts[0].endsWith(ARRAY_TOKEN)
                ? parts[0].substring(0, parts[0].length() - ARRAY_TOKEN.length())
                : parts[0]
                : null;

        return new ArrayPathParts(arrayPath, itemPath, rootKey);
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

    /**
     * Fallback for inconsistent schemas:
     * if "a.b.c" is missing, try "b.c", then "c".
     */
    private JsonNode readByPathWithFallback(JsonNode source, String path) {
        JsonNode exact = readByPath(source, path);
        if (exact != null) {
            return exact;
        }
        String[] parts = path.split("\\.");
        for (int i = 1; i < parts.length; i++) {
            String shortened = String.join(".", java.util.Arrays.copyOfRange(parts, i, parts.length));
            JsonNode candidate = readByPath(source, shortened);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
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

    private record ArrayPathParts(
            String arrayPath,
            String itemPath,
            String rootKey
    ) {
    }
}
