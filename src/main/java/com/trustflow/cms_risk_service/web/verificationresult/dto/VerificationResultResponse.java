package com.trustflow.cms_risk_service.web.verificationresult.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

public record VerificationResultResponse(
        UUID id,
        UUID companyId,
        long integrationId,
        String riskObjectId,
        String documentId,
        JsonNode data
) {
}
