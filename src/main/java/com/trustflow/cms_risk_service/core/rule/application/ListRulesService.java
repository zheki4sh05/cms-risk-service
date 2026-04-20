package com.trustflow.cms_risk_service.core.rule.application;

import com.trustflow.cms_risk_service.core.riskcategory.application.port.out.RiskCategoryRepository;
import com.trustflow.cms_risk_service.core.rule.application.port.in.ListRulesUseCase;
import com.trustflow.cms_risk_service.core.rule.application.port.out.RuleQueryRepository;
import com.trustflow.cms_risk_service.core.security.UserContext;
import com.trustflow.cms_risk_service.core.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListRulesService implements ListRulesUseCase {
    private final RuleQueryRepository ruleQueryRepository;
    private final RiskCategoryRepository riskCategoryRepository;
    private final RuleListMapper ruleListMapper;

    @Override
    public ListRulesResult listRules() {
        UserContext userContext = UserContextHolder.getRequired();
        Map<UUID, String> categoryLabels = riskCategoryRepository.findAllByCompanyId(userContext.companyId())
                .stream()
                .collect(Collectors.toMap(category -> category.id(), category -> category.name(), (left, right) -> left));

        return new ListRulesResult(
                ruleQueryRepository.findAllByCompanyId(userContext.companyId())
                        .stream()
                        .map(rule -> ruleListMapper.toItemResult(rule, categoryLabels.get(rule.categoryId())))
                        .toList()
        );
    }
}
