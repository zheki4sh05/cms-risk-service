package com.trustflow.cms_risk_service.infrastructure.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.trustflow.cms_risk_service.core.rule.application.port.out.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class AuthServiceUserInfoAdapter implements UserInfoRepository {
    private static final String UNKNOWN_AUTHOR_NAME = "Unknown user";

    private final RestClient authServiceRestClient;

    @Override
    public String findAuthorName(UUID userId, String accessToken) {
        try {
            UserInfoResponse response = authServiceRestClient.get()
                    .uri("/api/users/{id}", userId.toString())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(UserInfoResponse.class);
            return toAuthorName(response);
        } catch (RestClientException exception) {
            return UNKNOWN_AUTHOR_NAME;
        }
    }

    private String toAuthorName(UserInfoResponse response) {
        if (response == null) {
            return UNKNOWN_AUTHOR_NAME;
        }

        String fullName = Stream.of(response.lastName(), response.firstName())
                .filter(StringUtils::hasText)
                .map(String::trim)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
        if (StringUtils.hasText(fullName)) {
            return fullName;
        }
        if (StringUtils.hasText(response.username())) {
            return response.username().trim();
        }
        return UNKNOWN_AUTHOR_NAME;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UserInfoResponse(
            String username,
            String lastName,
            String firstName
    ) {
    }
}
