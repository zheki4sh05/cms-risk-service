package com.trustflow.cms_risk_service.web.verificationresult.dto;

import java.util.List;

public record ListVerificationResultsResponse(
        List<VerificationResultResponse> items
) {
}
