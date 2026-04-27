package com.trustflow.cms_risk_service.web.verificationresult.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertVerificationResultRequest(
        @NotNull(message = "integrationId is required")
        Long integrationId,
        @NotBlank(message = "riskObjectId is required")
        String riskObjectId,
        String documentId,
        @NotNull(message = "data is required")
        JsonNode data
) {
}
