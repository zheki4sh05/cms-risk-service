package com.trustflow.cms_risk_service.web.rule;

import com.trustflow.cms_risk_service.core.rule.application.CreateRuleCommand;
import com.trustflow.cms_risk_service.core.rule.application.CreateRuleResult;
import com.trustflow.cms_risk_service.core.rule.application.ListRulesResult;
import com.trustflow.cms_risk_service.core.rule.application.port.in.CreateRuleUseCase;
import com.trustflow.cms_risk_service.core.rule.application.port.in.ListRulesUseCase;
import com.trustflow.cms_risk_service.web.rule.dto.CreateRuleRequest;
import com.trustflow.cms_risk_service.web.rule.dto.CreateRuleResponse;
import com.trustflow.cms_risk_service.web.rule.dto.ListRulesResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class RuleController {
    private final CreateRuleUseCase createRuleUseCase;
    private final ListRulesUseCase listRulesUseCase;
    private final RuleWebMapper ruleWebMapper;

    @GetMapping("/api/rules")
    public ListRulesResponse listRules() {
        ListRulesResult result = listRulesUseCase.listRules();
        return ruleWebMapper.toListResponse(result);
    }

    @PostMapping("/api/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateRuleResponse createRule(
            @Valid @RequestBody CreateRuleRequest request
    ) {
        CreateRuleCommand command = ruleWebMapper.toCommand(request);
        CreateRuleResult result = createRuleUseCase.createRule(command);
        return ruleWebMapper.toResponse(result);
    }
}
