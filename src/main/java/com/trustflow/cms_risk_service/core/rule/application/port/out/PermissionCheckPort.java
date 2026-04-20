package com.trustflow.cms_risk_service.core.rule.application.port.out;

import com.trustflow.cms_risk_service.core.security.Permission;

import java.util.UUID;

public interface PermissionCheckPort {
    boolean hasPermission(UUID userId, String accessToken, Permission permission);
}
