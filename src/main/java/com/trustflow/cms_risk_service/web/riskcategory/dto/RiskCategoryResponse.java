package com.trustflow.cms_risk_service.web.riskcategory.dto;

import java.util.UUID;

public record RiskCategoryResponse(
        UUID id,
        String name
) {
}
