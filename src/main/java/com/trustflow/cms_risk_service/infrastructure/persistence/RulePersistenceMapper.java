package com.trustflow.cms_risk_service.infrastructure.persistence;

import com.trustflow.cms_risk_service.core.rule.domain.Rule;
import com.trustflow.cms_risk_service.core.rule.domain.RuleAction;
import com.trustflow.cms_risk_service.core.rule.domain.RulePriority;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.RuleJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RulePersistenceMapper {
    @Mapping(target = "priority", expression = "java(rule.priority().value())")
    @Mapping(target = "actions", source = "actions")
    @Mapping(target = "successCount", ignore = true)
    @Mapping(target = "triggersCount", ignore = true)
    @Mapping(target = "failedCount", ignore = true)
    @Mapping(target = "lastDateInvocation", ignore = true)
    @Mapping(target = "lastDateTrigger", ignore = true)
    RuleJpaEntity toEntity(Rule rule);

    @Mapping(target = "priority", expression = "java(toPriority(entity.getPriority()))")
    @Mapping(target = "actions", source = "actions")
    Rule toDomain(RuleJpaEntity entity);

    default List<String> mapActions(List<RuleAction> actions) {
        return actions.stream().map(RuleAction::value).toList();
    }

    default List<RuleAction> mapActionValues(List<String> actions) {
        return actions.stream().map(RuleAction::fromValue).toList();
    }

    default RulePriority toPriority(String value) {
        return RulePriority.fromValue(value);
    }
}
