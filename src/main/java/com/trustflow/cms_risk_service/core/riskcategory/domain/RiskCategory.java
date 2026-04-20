package com.trustflow.cms_risk_service.core.riskcategory.domain;

import java.util.UUID;

public record RiskCategory(
        UUID id,
        UUID companyId,
        String name
) {
}
