package com.trustflow.cms_risk_service.web.riskcategory.dto;

import jakarta.validation.constraints.NotNull;

public record UpsertRiskCategoryRequest(
        @NotNull(message = "name is required")
        String name
) {
}
