package com.trustflow.cms_risk_service.core.security;

public enum Permission {
    MANAGE_RULES_AND_RISKS("MANAGE_RULES_AND_RISKS");

    private final String value;

    Permission(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
