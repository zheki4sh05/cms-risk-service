package com.trustflow.cms_risk_service.core.rule.application.port.in;

import com.trustflow.cms_risk_service.core.rule.application.RuleChangeHistoryDetailsResult;
import com.trustflow.cms_risk_service.core.rule.application.RuleChangeHistoryResult;

import java.util.UUID;

public interface GetRuleChangeHistoryUseCase {
    RuleChangeHistoryResult getRuleChangeHistory(int page, int pageSize, String query);

    RuleChangeHistoryDetailsResult getRuleChangeHistoryDetails(UUID historyId);
}
