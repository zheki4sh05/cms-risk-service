package com.trustflow.cms_risk_service.core.security;

import com.trustflow.cms_risk_service.core.common.exception.UnauthorizedException;

public final class UserContextHolder {
    private static final ThreadLocal<UserContext> CURRENT_USER = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(UserContext context) {
        CURRENT_USER.set(context);
    }

    public static UserContext getRequired() {
        UserContext context = CURRENT_USER.get();
        if (context == null) {
            throw new UnauthorizedException("User context is not initialized");
        }
        return context;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
