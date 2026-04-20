package com.trustflow.cms_risk_service.core.rule.application;

import com.trustflow.cms_risk_service.core.rule.domain.Rule;
import com.trustflow.cms_risk_service.core.rule.domain.RuleAction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RuleListMapper {
    @Mapping(target = "action", source = "rule.actions", qualifiedByName = "toActionLabel")
    @Mapping(target = "priority", expression = "java(rule.priority().value())")
    @Mapping(target = "categoryLabel", source = "categoryLabel")
    RuleListItemResult toItemResult(Rule rule, String categoryLabel);

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
