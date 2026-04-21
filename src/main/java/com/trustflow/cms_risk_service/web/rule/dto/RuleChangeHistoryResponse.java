package com.trustflow.cms_risk_service.web.rule.dto;

import java.util.List;

public record RuleChangeHistoryResponse(
        List<RuleChangeHistoryItemResponse> items,
        boolean hasMore
) {
}
