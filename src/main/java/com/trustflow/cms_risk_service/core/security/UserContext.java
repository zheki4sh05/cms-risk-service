package com.trustflow.cms_risk_service.core.security;

import java.util.UUID;

public record UserContext(
        UUID userId,
        UUID companyId,
        String accessToken,
        String subject
) {
}
