package com.trustflow.cms_risk_service.web.rule;

import com.trustflow.cms_risk_service.core.rule.application.CreateRuleCommand;
import com.trustflow.cms_risk_service.core.rule.application.CreateRuleResult;
import com.trustflow.cms_risk_service.core.rule.application.UpdateRuleCommand;
import com.trustflow.cms_risk_service.core.rule.application.UpdateRuleResult;
import com.trustflow.cms_risk_service.core.rule.application.RuleChangeHistoryDetailsResult;
import com.trustflow.cms_risk_service.core.rule.application.ListRulesResult;
import com.trustflow.cms_risk_service.core.rule.application.RuleChangeHistoryItemResult;
import com.trustflow.cms_risk_service.core.rule.application.RuleChangeHistoryResult;
import com.trustflow.cms_risk_service.core.rule.application.RuleDetailsResult;
import com.trustflow.cms_risk_service.core.rule.domain.RuleAction;
import com.trustflow.cms_risk_service.web.rule.dto.CreateRuleRequest;
import com.trustflow.cms_risk_service.web.rule.dto.CreateRuleResponse;
import com.trustflow.cms_risk_service.web.rule.dto.ListRulesResponse;
import com.trustflow.cms_risk_service.web.rule.dto.RuleChangeHistoryDetailsResponse;
import com.trustflow.cms_risk_service.web.rule.dto.RuleChangeHistoryItemResponse;
import com.trustflow.cms_risk_service.web.rule.dto.RuleChangeHistoryResponse;
import com.trustflow.cms_risk_service.web.rule.dto.RuleDetailsResponse;
import com.trustflow.cms_risk_service.web.rule.dto.UpdateRuleRequest;
import com.trustflow.cms_risk_service.web.rule.dto.UpdateRuleResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface RuleWebMapper {
    CreateRuleCommand toCommand(CreateRuleRequest request);

    @Mapping(target = "savedAt", expression = "java(result.savedAt().toString())")
    CreateRuleResponse toResponse(CreateRuleResult result);

    UpdateRuleCommand toCommand(UUID id, UpdateRuleRequest request);

    @Mapping(target = "savedAt", expression = "java(result.savedAt().toString())")
    UpdateRuleResponse toResponse(UpdateRuleResult result);

    ListRulesResponse toListResponse(ListRulesResult result);

    RuleChangeHistoryResponse toChangeHistoryResponse(RuleChangeHistoryResult result);

    @Mapping(target = "changedAt", expression = "java(result.changedAt().toString())")
    RuleChangeHistoryItemResponse toChangeHistoryItemResponse(RuleChangeHistoryItemResult result);

    @Mapping(target = "changedAt", expression = "java(result.changedAt().toString())")
    @Mapping(target = "savedAt", expression = "java(result.savedAt() == null ? null : result.savedAt().toString())")
    RuleChangeHistoryDetailsResponse toChangeHistoryDetailsResponse(RuleChangeHistoryDetailsResult result);

    @Mapping(target = "priority", expression = "java(result.priority().value())")
    @Mapping(target = "actions", source = "actions")
    @Mapping(target = "savedAt", expression = "java(result.savedAt().toString())")
    RuleDetailsResponse toDetailsResponse(RuleDetailsResult result);

    default List<String> mapActions(List<RuleAction> actions) {
        return actions.stream().map(RuleAction::value).toList();
    }
}
