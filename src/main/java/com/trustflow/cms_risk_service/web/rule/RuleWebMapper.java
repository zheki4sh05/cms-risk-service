package com.trustflow.cms_risk_service.web.rule;

import com.trustflow.cms_risk_service.core.rule.application.CreateRuleCommand;
import com.trustflow.cms_risk_service.core.rule.application.CreateRuleResult;
import com.trustflow.cms_risk_service.core.rule.application.ListRulesResult;
import com.trustflow.cms_risk_service.web.rule.dto.CreateRuleRequest;
import com.trustflow.cms_risk_service.web.rule.dto.CreateRuleResponse;
import com.trustflow.cms_risk_service.web.rule.dto.ListRulesResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RuleWebMapper {
    CreateRuleCommand toCommand(CreateRuleRequest request);

    @Mapping(target = "savedAt", expression = "java(result.savedAt().toString())")
    CreateRuleResponse toResponse(CreateRuleResult result);

    ListRulesResponse toListResponse(ListRulesResult result);
}
