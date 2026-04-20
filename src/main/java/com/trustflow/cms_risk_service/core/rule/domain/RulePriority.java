package com.trustflow.cms_risk_service.core.rule.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RulePriority {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    private final String value;

    RulePriority(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static RulePriority fromValue(String value) {
        for (RulePriority priority : values()) {
            if (priority.value.equals(value)) {
                return priority;
            }
        }
        throw new IllegalArgumentException("Unsupported priority: " + value);
    }
}
