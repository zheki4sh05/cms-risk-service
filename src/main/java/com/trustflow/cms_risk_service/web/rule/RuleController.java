package com.trustflow.cms_risk_service.web.rule;

import com.trustflow.cms_risk_service.core.rule.application.CreateRuleCommand;
import com.trustflow.cms_risk_service.core.rule.application.CreateRuleResult;
import com.trustflow.cms_risk_service.core.rule.application.ListRulesResult;
import com.trustflow.cms_risk_service.core.rule.application.RuleChangeHistoryDetailsResult;
import com.trustflow.cms_risk_service.core.rule.application.RuleChangeHistoryResult;
import com.trustflow.cms_risk_service.core.rule.application.RuleDetailsResult;
import com.trustflow.cms_risk_service.core.rule.application.UpdateRuleCommand;
import com.trustflow.cms_risk_service.core.rule.application.UpdateRuleRiskObjectCommand;
import com.trustflow.cms_risk_service.core.rule.application.UpdateRuleResult;
import com.trustflow.cms_risk_service.core.rule.application.port.in.CreateRuleUseCase;
import com.trustflow.cms_risk_service.core.rule.application.port.in.GetRuleChangeHistoryUseCase;
import com.trustflow.cms_risk_service.core.rule.application.port.in.GetRuleDetailsUseCase;
import com.trustflow.cms_risk_service.core.rule.application.port.in.ListRulesUseCase;
import com.trustflow.cms_risk_service.core.rule.application.port.in.UpdateRuleRiskObjectUseCase;
import com.trustflow.cms_risk_service.core.rule.application.port.in.UpdateRuleUseCase;
import com.trustflow.cms_risk_service.web.rule.dto.CreateRuleRequest;
import com.trustflow.cms_risk_service.web.rule.dto.CreateRuleResponse;
import com.trustflow.cms_risk_service.web.rule.dto.ListRulesResponse;
import com.trustflow.cms_risk_service.web.rule.dto.RuleChangeHistoryDetailsResponse;
import com.trustflow.cms_risk_service.web.rule.dto.RuleChangeHistoryResponse;
import com.trustflow.cms_risk_service.web.rule.dto.RuleDetailsResponse;
import com.trustflow.cms_risk_service.web.rule.dto.RuleShortResponse;
import com.trustflow.cms_risk_service.web.rule.dto.UpdateRuleRequest;
import com.trustflow.cms_risk_service.web.rule.dto.UpdateRuleRiskObjectRequest;
import com.trustflow.cms_risk_service.web.rule.dto.UpdateRuleResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class RuleController {
    private final CreateRuleUseCase createRuleUseCase;
    private final ListRulesUseCase listRulesUseCase;
    private final GetRuleDetailsUseCase getRuleDetailsUseCase;
    private final UpdateRuleUseCase updateRuleUseCase;
    private final UpdateRuleRiskObjectUseCase updateRuleRiskObjectUseCase;
    private final GetRuleChangeHistoryUseCase getRuleChangeHistoryUseCase;
    private final RuleWebMapper ruleWebMapper;

    @Operation(summary = "Returns the list of all configured risk rules.")
    @GetMapping("/api/rules")
    public ListRulesResponse listRules() {
        ListRulesResult result = listRulesUseCase.listRules();
        return ruleWebMapper.toListResponse(result);
    }

    @Operation(summary = "Returns full details of a specific risk rule by ID.")
    @GetMapping("/api/rules/{id}")
    public RuleDetailsResponse getRuleDetails(@PathVariable("id") UUID ruleId) {
        RuleDetailsResult result = getRuleDetailsUseCase.getRuleDetails(ruleId);
        return ruleWebMapper.toDetailsResponse(result);
    }

    @Operation(summary = "Returns short details of a specific risk rule by ID.")
    @GetMapping("/api/rules/short/{id}")
    public RuleShortResponse getRuleShortDetails(@PathVariable("id") UUID ruleId) {
        RuleDetailsResult result = getRuleDetailsUseCase.getRuleDetails(ruleId);
        return ruleWebMapper.toShortResponse(result);
    }

    @Operation(summary = "Returns paginated change history for risk rules with optional search.")
    @GetMapping("/api/rules/change-history")
    public RuleChangeHistoryResponse getRuleChangeHistory(
            @RequestParam("page") int page,
            @RequestParam("pageSize") int pageSize,
            @RequestParam(value = "q", required = false) String query
    ) {
        RuleChangeHistoryResult result = getRuleChangeHistoryUseCase.getRuleChangeHistory(page, pageSize, query);
        return ruleWebMapper.toChangeHistoryResponse(result);
    }

    @Operation(summary = "Returns details of a specific rule change history record.")
    @GetMapping("/api/rules/change-history/{id}")
    public RuleChangeHistoryDetailsResponse getRuleChangeHistoryDetails(@PathVariable("id") UUID historyId) {
        RuleChangeHistoryDetailsResult result = getRuleChangeHistoryUseCase.getRuleChangeHistoryDetails(historyId);
        return ruleWebMapper.toChangeHistoryDetailsResponse(result);
    }

    @Operation(summary = "Creates a new risk rule from the provided payload.")
    @PostMapping("/api/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateRuleResponse createRule(
            @Valid @RequestBody CreateRuleRequest request
    ) {
        CreateRuleCommand command = ruleWebMapper.toCommand(request);
        CreateRuleResult result = createRuleUseCase.createRule(command);
        return ruleWebMapper.toResponse(result);
    }

    @Operation(summary = "Updates an existing risk rule by its ID.")
    @PutMapping("/api/rules/{id}")
    public UpdateRuleResponse updateRule(
            @PathVariable("id") UUID ruleId,
            @Valid @RequestBody UpdateRuleRequest request
    ) {
        log.debug("PUT /api/rules/{} received update request", ruleId);
        UpdateRuleCommand command = ruleWebMapper.toCommand(ruleId, request);
        UpdateRuleResult result = updateRuleUseCase.updateRule(command);
        log.debug("PUT /api/rules/{} updated successfully at {}", ruleId, result.savedAt());
        return ruleWebMapper.toResponse(result);
    }

    @Operation(summary = "Updates only the risk object binding for a specific rule.")
    @PutMapping("/api/rules/{id}/risk-object")
    public UpdateRuleResponse updateRuleRiskObject(
            @PathVariable("id") UUID ruleId,
            @RequestBody UpdateRuleRiskObjectRequest request
    ) {
        UpdateRuleRiskObjectCommand command = ruleWebMapper.toRiskObjectCommand(ruleId, request);
        UpdateRuleResult result = updateRuleRiskObjectUseCase.updateRuleRiskObject(command);
        return ruleWebMapper.toResponse(result);
    }
}
