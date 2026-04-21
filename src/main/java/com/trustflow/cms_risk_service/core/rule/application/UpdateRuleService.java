package com.trustflow.cms_risk_service.core.rule.application;

import com.trustflow.cms_risk_service.core.common.exception.ForbiddenException;
import com.trustflow.cms_risk_service.core.common.exception.NotFoundException;
import com.trustflow.cms_risk_service.core.rule.application.port.in.UpdateRuleUseCase;
import com.trustflow.cms_risk_service.core.rule.application.port.out.PermissionCheckPort;
import com.trustflow.cms_risk_service.core.rule.application.port.out.RuleCommandRepository;
import com.trustflow.cms_risk_service.core.rule.application.port.out.RuleQueryRepository;
import com.trustflow.cms_risk_service.core.rule.domain.Rule;
import com.trustflow.cms_risk_service.core.security.Permission;
import com.trustflow.cms_risk_service.core.security.UserContext;
import com.trustflow.cms_risk_service.core.security.UserContextHolder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateRuleService implements UpdateRuleUseCase {
    private static final String RULE_NOT_FOUND_MESSAGE = "Правило не найдено";

    private final RuleCommandRepository ruleCommandRepository;
    private final RuleQueryRepository ruleQueryRepository;
    private final PermissionCheckPort permissionCheckPort;
    private final Clock clock;

    @Override
    @Transactional
    public UpdateRuleResult updateRule(UpdateRuleCommand command) {
        UserContext userContext = UserContextHolder.getRequired();
        log.debug("Updating rule: ruleId={} companyId={} requestedBy={}", command.id(), userContext.companyId(), userContext.userId());
        boolean hasPermission = permissionCheckPort.hasPermission(
                userContext.userId(),
                userContext.accessToken(),
                Permission.MANAGE_RULES_AND_RISKS
        );
        if (!hasPermission) {
            log.debug("Rule update forbidden: ruleId={} requestedBy={}", command.id(), userContext.userId());
            throw new ForbiddenException("User has no permission: " + Permission.MANAGE_RULES_AND_RISKS.value());
        }

        Rule existingRule = ruleQueryRepository.findByIdAndCompanyId(command.id(), userContext.companyId())
                .orElseThrow(() -> new NotFoundException(RULE_NOT_FOUND_MESSAGE));

        Instant now = Instant.now(clock);
        Rule updatedRule = new Rule(
                existingRule.id(),
                existingRule.companyId(),
                command.name(),
                command.condition(),
                command.categoryId(),
                command.riskObjectId(),
                command.priority(),
                command.responsibleUserId(),
                command.actions(),
                command.enabled(),
                command.mechanismScriptName(),
                command.mechanismScriptContent(),
                existingRule.createdByUserId(),
                now
        );

        Rule savedRule = ruleCommandRepository.save(updatedRule);
        log.debug("Rule entity saved: ruleId={} savedAt={}", savedRule.id(), savedRule.savedAt());
        ruleCommandRepository.saveHistory(new RuleHistoryWriteCommand(
                UUID.randomUUID(),
                savedRule.companyId(),
                savedRule.id(),
                savedRule.name(),
                command.description(),
                userContext.userId(),
                savedRule.condition(),
                savedRule.categoryId(),
                savedRule.riskObjectId(),
                savedRule.priority().value(),
                savedRule.responsibleUserId(),
                savedRule.actions(),
                savedRule.enabled(),
                savedRule.mechanismScriptName(),
                savedRule.mechanismScriptContent(),
                savedRule.createdByUserId(),
                savedRule.savedAt(),
                now
        ));
        log.debug("Rule history saved: ruleId={} historyAuthorId={}", savedRule.id(), userContext.userId());

        return new UpdateRuleResult(savedRule.id(), savedRule.savedAt());
    }
}
