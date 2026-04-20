package com.trustflow.cms_risk_service.web.riskcategory.dto;

import java.util.List;

public record ListRiskCategoriesResponse(
        List<RiskCategoryResponse> items
) {
}
