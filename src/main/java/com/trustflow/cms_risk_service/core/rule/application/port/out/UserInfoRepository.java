package com.trustflow.cms_risk_service.core.rule.application.port.out;

import java.util.UUID;

public interface UserInfoRepository {
    String findAuthorName(UUID userId, String accessToken);
}
