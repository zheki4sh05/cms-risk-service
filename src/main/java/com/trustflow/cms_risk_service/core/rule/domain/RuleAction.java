package com.trustflow.cms_risk_service.core.rule.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RuleAction {
    CREATE_INCIDENT("createIncident"),
    SEND_NOTIFICATION("sendNotification");

    private final String value;

    RuleAction(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static RuleAction fromValue(String value) {
        for (RuleAction action : values()) {
            if (action.value.equals(value)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unsupported action: " + value);
    }
}
