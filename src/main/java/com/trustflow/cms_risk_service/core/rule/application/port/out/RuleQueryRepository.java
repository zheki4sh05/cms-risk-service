package com.trustflow.cms_risk_service.core.rule.application.port.out;

import com.trustflow.cms_risk_service.core.rule.application.RuleChangeHistoryDetailsResult;
import com.trustflow.cms_risk_service.core.rule.domain.Rule;
import com.trustflow.cms_risk_service.core.rule.application.RuleChangeHistoryPageResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RuleQueryRepository {
    List<Rule> findAllByCompanyId(UUID companyId);

    Optional<Rule> findByIdAndCompanyId(UUID id, UUID companyId);

    RuleChangeHistoryPageResult findChangeHistoryByCompanyId(UUID companyId, String query, int page, int pageSize);

    Optional<RuleChangeHistoryDetailsResult> findChangeHistoryDetailsByIdAndCompanyId(UUID id, UUID companyId);
}
