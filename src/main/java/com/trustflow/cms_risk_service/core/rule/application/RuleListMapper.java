package com.trustflow.cms_risk_service.core.rule.application;

import com.trustflow.cms_risk_service.core.rule.domain.Rule;
import com.trustflow.cms_risk_service.core.rule.domain.RuleAction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RuleListMapper {
    @Mapping(target = "id", source = "rule.id")
    @Mapping(target = "name", source = "rule.name")
    @Mapping(target = "condition", source = "rule.condition")
    @Mapping(target = "action", source = "rule.actions", qualifiedByName = "toActionLabel")
    @Mapping(target = "categoryId", source = "rule.categoryId")
    @Mapping(target = "priority", expression = "java(rule.priority().value())")
    @Mapping(target = "enabled", source = "rule.enabled")
    @Mapping(target = "riskObjectId", source = "rule.riskObjectId")
    @Mapping(target = "categoryLabel", source = "categoryLabel")
    @Mapping(target = "riskObject", source = "riskObject")
    RuleListItemResult toItemResult(Rule rule, String categoryLabel, RiskObjectResult riskObject);

    @Named("toActionLabel")
    default String toActionLabel(List<RuleAction> actions) {
        if (actions == null || actions.isEmpty()) {
            return null;
        }
        RuleAction primaryAction = actions.getFirst();
        return switch (primaryAction) {
            case CREATE_INCIDENT -> "Создать инцидент";
            case SEND_NOTIFICATION -> "Отправить уведомление";
        };
    }
}
