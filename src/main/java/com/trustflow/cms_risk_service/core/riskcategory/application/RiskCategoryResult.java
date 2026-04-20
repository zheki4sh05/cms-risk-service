package com.trustflow.cms_risk_service.core.riskcategory.application;

import java.util.UUID;

public record RiskCategoryResult(
        UUID id,
        String name
) {
}
