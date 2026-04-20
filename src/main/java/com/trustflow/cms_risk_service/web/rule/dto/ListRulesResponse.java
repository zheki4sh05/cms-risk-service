package com.trustflow.cms_risk_service.web.rule.dto;

import java.util.List;

public record ListRulesResponse(
        List<RuleItemResponse> items
) {
}
