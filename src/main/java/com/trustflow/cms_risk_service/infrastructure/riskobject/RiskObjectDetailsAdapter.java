package com.trustflow.cms_risk_service.infrastructure.riskobject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.trustflow.cms_risk_service.core.rule.application.RiskObjectResult;
import com.trustflow.cms_risk_service.core.rule.application.port.out.RiskObjectDetailsRepository;
import com.trustflow.cms_risk_service.core.security.UserContext;
import com.trustflow.cms_risk_service.core.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskObjectDetailsAdapter implements RiskObjectDetailsRepository {
    private static final String COMPANY_ID_HEADER = "CompanyId";

    private final RestClient riskObjectServiceRestClient;

    @Override
    public Map<UUID, RiskObjectResult> findByIds(Set<UUID> ids) {
        UserContext userContext = UserContextHolder.getRequired();
        Map<UUID, RiskObjectResult> result = new HashMap<>();
        log.debug(
                "Starting risk-object enrichment for rules list: idsCount={}, companyId={}, userId={}",
                ids.size(),
                userContext.companyId(),
                userContext.userId()
        );
        for (UUID id : ids) {
            try {
                String authHeader = "Bearer " + userContext.accessToken();
                String companyIdHeaderValue = userContext.companyId().toString();
                log.debug(
                        "Outgoing request headers for risk-object details: Authorization={}, CompanyId={}",
                        maskAuthorizationHeader(authHeader),
                        companyIdHeaderValue
                );
                log.debug(
                        "Requesting risk-object details: endpoint=/api/internal/risk-objects/{id}, riskObjectId={}, headers=[Authorization=Bearer ***, CompanyId={}]",
                        id,
                        companyIdHeaderValue
                );
                RiskObjectResponse response = riskObjectServiceRestClient.get()
                        .uri("/api/internal/risk-objects/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .header(COMPANY_ID_HEADER, companyIdHeaderValue)
                        .retrieve()
                        .body(RiskObjectResponse.class);
                if (response != null) {
                    result.put(id, new RiskObjectResult(
                            response.id(),
                            response.uuid(),
                            response.code(),
                            response.name(),
                            response.status(),
                            response.updatedAt(),
                            response.definition()
                    ));
                    log.debug(
                            "Risk-object details loaded successfully: requestedRiskObjectId={}, response.id={}, response.uuid={}, response.code={}, response.status={}",
                            id,
                            response.id(),
                            response.uuid(),
                            response.code(),
                            response.status()
                    );
                } else {
                    log.warn(
                            "Risk-object service returned empty body: requestedRiskObjectId={}, companyId={}. " +
                                    "Rule item will be returned with riskObject=null.",
                            id,
                            userContext.companyId()
                    );
                }
            } catch (RestClientResponseException exception) {
                log.warn(
                        "Failed to load risk object details (HTTP error): requestedRiskObjectId={}, companyId={}, statusCode={}, statusText={}, responseBody={}. " +
                                "Reason: risk-object service did not return a valid object for this identifier in the current tenant context. " +
                                "Impact: /api/rules remains available and returns 200, but this specific rule item will have riskObject=null.",
                        id,
                        userContext.companyId(),
                        exception.getStatusCode().value(),
                        exception.getStatusText(),
                        exception.getResponseBodyAsString()
                );
            } catch (RestClientException exception) {
                log.warn(
                        "Failed to load risk object details (transport/client error): requestedRiskObjectId={}, companyId={}, errorClass={}, message={}. " +
                                "Reason: request could not be completed (network/timeout/client). " +
                                "Impact: /api/rules remains available and returns 200, but this specific rule item will have riskObject=null.",
                        id,
                        userContext.companyId(),
                        exception.getClass().getSimpleName(),
                        exception.getMessage()
                );
            }
        }
        log.debug(
                "Completed risk-object enrichment for rules list: requestedIdsCount={}, loadedIdsCount={}, missingIdsCount={}",
                ids.size(),
                result.size(),
                ids.size() - result.size()
        );
        return result;
    }

    private String maskAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return "<empty>";
        }
        return "Bearer ***";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RiskObjectResponse(
            String id,
            String uuid,
            String code,
            String name,
            String status,
            Instant updatedAt,
            String definition
    ) {
    }
}
