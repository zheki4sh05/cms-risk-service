package com.trustflow.cms_risk_service.core.rule.application.port.out;

import com.trustflow.cms_risk_service.core.rule.domain.Rule;

import java.util.List;
import java.util.UUID;

public interface RuleQueryRepository {
    List<Rule> findAllByCompanyId(UUID companyId);
}
