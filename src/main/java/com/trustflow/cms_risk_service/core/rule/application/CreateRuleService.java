package com.trustflow.cms_risk_service.core.rule.application;

import com.trustflow.cms_risk_service.core.common.exception.ForbiddenException;
import com.trustflow.cms_risk_service.core.security.Permission;
import com.trustflow.cms_risk_service.core.security.UserContext;
import com.trustflow.cms_risk_service.core.security.UserContextHolder;
import com.trustflow.cms_risk_service.core.rule.application.port.in.CreateRuleUseCase;
import com.trustflow.cms_risk_service.core.rule.application.port.out.PermissionCheckPort;
import com.trustflow.cms_risk_service.core.rule.application.port.out.RuleCommandRepository;
import com.trustflow.cms_risk_service.core.rule.domain.Rule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateRuleService implements CreateRuleUseCase {
    private final RuleCommandRepository ruleCommandRepository;
    private final PermissionCheckPort permissionCheckPort;
    private final RuleDomainMapper ruleDomainMapper;
    private final Clock clock;

    @Override
    public CreateRuleResult createRule(CreateRuleCommand command) {
        UserContext userContext = UserContextHolder.getRequired();
        boolean hasPermission = permissionCheckPort.hasPermission(
                userContext.userId(),
                userContext.accessToken(),
                Permission.MANAGE_RULES_AND_RISKS
        );
        if (!hasPermission) {
            throw new ForbiddenException("User has no permission: " + Permission.MANAGE_RULES_AND_RISKS.value());
        }

        Rule rule = ruleDomainMapper.toRule(
                command,
                UUID.randomUUID(),
                userContext.companyId(),
                userContext.userId(),
                Instant.now(clock)
        );

        Rule savedRule = ruleCommandRepository.save(rule);
        return new CreateRuleResult(savedRule.id(), savedRule.savedAt());
    }
}
