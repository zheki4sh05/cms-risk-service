package com.trustflow.cms_risk_service.infrastructure.auth;

import com.trustflow.cms_risk_service.core.rule.application.port.out.PermissionCheckPort;
import com.trustflow.cms_risk_service.core.security.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthServicePermissionAdapter implements PermissionCheckPort {
    private final RestClient authServiceRestClient;

    @Override
    public boolean hasPermission(UUID userId, String accessToken, Permission permission) {
        try {
            PermissionCheckResponse response = authServiceRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/users/{id}/permissions/check")
                            .queryParam("permission", permission.value())
                            .build(userId.toString()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(PermissionCheckResponse.class);

            return response != null && "permit".equalsIgnoreCase(response.access());
        } catch (RestClientException exception) {
            return false;
        }
    }

    private record PermissionCheckResponse(String access) {
    }
}
