package com.trustflow.cms_risk_service.core.riskcategory.application;

import java.util.List;

public record ListRiskCategoriesResult(
        List<RiskCategoryResult> items
) {
}
