package com.trustflow.cms_risk_service.core.rule.application;

import java.util.List;

public record ListRulesResult(
        List<RuleListItemResult> items
) {
}
