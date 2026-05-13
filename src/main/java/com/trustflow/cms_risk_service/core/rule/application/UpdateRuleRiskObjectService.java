package com.trustflow.cms_risk_service.core.rule.application;

import com.trustflow.cms_risk_service.core.common.exception.ForbiddenException;
import com.trustflow.cms_risk_service.core.common.exception.NotFoundException;
import com.trustflow.cms_risk_service.core.rule.application.port.in.UpdateRuleRiskObjectUseCase;
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
public class UpdateRuleRiskObjectService implements UpdateRuleRiskObjectUseCase {
    private static final String RULE_NOT_FOUND_MESSAGE = "Правило не найдено";

    private final RuleCommandRepository ruleCommandRepository;
    private final RuleQueryRepository ruleQueryRepository;
    private final PermissionCheckPort permissionCheckPort;
    private final Clock clock;

    @Override
    @Transactional
    public UpdateRuleResult updateRuleRiskObject(UpdateRuleRiskObjectCommand command) {
        UserContext userContext = UserContextHolder.getRequired();
        boolean hasPermission = permissionCheckPort.hasPermission(
                userContext.userId(),
                userContext.accessToken(),
                Permission.MANAGE_RULES_AND_RISKS
        );
        if (!hasPermission) {
            throw new ForbiddenException("User has no permission: " + Permission.MANAGE_RULES_AND_RISKS.value());
        }

        Rule existingRule = ruleQueryRepository.findByIdAndCompanyId(command.id(), userContext.companyId())
                .orElseThrow(() -> new NotFoundException(RULE_NOT_FOUND_MESSAGE));

        Instant now = Instant.now(clock);
        Rule updatedRule = new Rule(
                existingRule.id(),
                existingRule.companyId(),
                existingRule.name(),
                existingRule.condition(),
                existingRule.categoryId(),
                command.riskObjectId(),
                existingRule.priority(),
                existingRule.responsibleUserId(),
                existingRule.actions(),
                existingRule.enabled(),
                existingRule.mechanismScriptName(),
                existingRule.mechanismScriptContent(),
                existingRule.createdByUserId(),
                now
        );

        Rule savedRule = ruleCommandRepository.save(updatedRule);

        ruleCommandRepository.saveHistory(new RuleHistoryWriteCommand(
                UUID.randomUUID(),
                existingRule.companyId(),
                existingRule.id(),
                existingRule.name(),
                "",
                userContext.userId(),
                existingRule.condition(),
                existingRule.categoryId(),
                existingRule.riskObjectId(),
                existingRule.priority().value(),
                existingRule.responsibleUserId(),
                existingRule.actions(),
                existingRule.enabled(),
                existingRule.mechanismScriptName(),
                existingRule.mechanismScriptContent(),
                existingRule.createdByUserId(),
                existingRule.savedAt(),
                now
        ));

        log.debug("Rule riskObjectId updated: ruleId={} riskObjectId={} savedAt={}",
                savedRule.id(), savedRule.riskObjectId(), savedRule.savedAt());
        return new UpdateRuleResult(savedRule.id(), savedRule.savedAt());
    }
}
