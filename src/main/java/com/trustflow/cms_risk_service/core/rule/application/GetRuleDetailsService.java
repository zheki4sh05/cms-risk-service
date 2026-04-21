package com.trustflow.cms_risk_service.core.rule.application;

import com.trustflow.cms_risk_service.core.common.exception.NotFoundException;
import com.trustflow.cms_risk_service.core.rule.application.port.in.GetRuleDetailsUseCase;
import com.trustflow.cms_risk_service.core.rule.application.port.out.RuleQueryRepository;
import com.trustflow.cms_risk_service.core.rule.domain.Rule;
import com.trustflow.cms_risk_service.core.security.UserContext;
import com.trustflow.cms_risk_service.core.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetRuleDetailsService implements GetRuleDetailsUseCase {
    private static final String RULE_NOT_FOUND_MESSAGE = "Правило не найдено";

    private final RuleQueryRepository ruleQueryRepository;

    @Override
    public RuleDetailsResult getRuleDetails(UUID ruleId) {
        UserContext userContext = UserContextHolder.getRequired();
        Rule rule = ruleQueryRepository.findByIdAndCompanyId(ruleId, userContext.companyId())
                .orElseThrow(() -> new NotFoundException(RULE_NOT_FOUND_MESSAGE));

        return new RuleDetailsResult(
                rule.id(),
                rule.companyId(),
                rule.name(),
                rule.condition(),
                rule.categoryId(),
                rule.riskObjectId(),
                rule.priority(),
                rule.responsibleUserId(),
                rule.actions(),
                rule.enabled(),
                rule.mechanismScriptName(),
                rule.mechanismScriptContent(),
                rule.createdByUserId(),
                rule.savedAt()
        );
    }
}
