package com.trustflow.cms_risk_service.core.rule.application;

import com.trustflow.cms_risk_service.core.rule.domain.Rule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface RuleDomainMapper {
    @Mapping(target = "id", source = "id")
    @Mapping(target = "companyId", source = "companyId")
    @Mapping(target = "createdByUserId", source = "createdByUserId")
    @Mapping(target = "savedAt", source = "savedAt")
    Rule toRule(
            CreateRuleCommand command,
            UUID id,
            UUID companyId,
            UUID createdByUserId,
            Instant savedAt
    );
}
