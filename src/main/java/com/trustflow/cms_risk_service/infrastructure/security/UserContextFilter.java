package com.trustflow.cms_risk_service.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustflow.cms_risk_service.core.security.UserContext;
import com.trustflow.cms_risk_service.core.security.UserContextHolder;
import com.trustflow.cms_risk_service.web.common.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class UserContextFilter extends OncePerRequestFilter {
    private static final String COMPANY_ID_HEADER = "CompanyId";
    private static final String COMPANY_ID_LEGACY_HEADER = "companyId";

    private final ObjectMapper objectMapper;

    public UserContextFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/swagger-ui.html".equals(path)
                || path.startsWith("/swagger-ui/")
                || "/v3/api-docs".equals(path)
                || path.startsWith("/v3/api-docs/")
                || "/v3/api-docs.yaml".equals(path)
                || path.startsWith("/webjars/")
                || "/error".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                UUID userId = extractUserId(jwt);
                UUID companyId = extractCompanyId(request, jwt);
                UserContextHolder.set(new UserContext(userId, companyId, jwt.getTokenValue(), jwt.getSubject()));
            }
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException exception) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } finally {
            UserContextHolder.clear();
        }
    }

    private UUID extractUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaims().get("userId");
        if (userIdClaim instanceof String userIdValue && !userIdValue.isBlank()) {
            return parseUuid(userIdValue, "userId claim must be valid UUID");
        }
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Unable to resolve userId from access token");
        }
        return parseUuid(subject, "JWT subject must be valid UUID");
    }

    private UUID extractCompanyId(HttpServletRequest request, Jwt jwt) {
        String companyHeader = request.getHeader(COMPANY_ID_HEADER);
        if (companyHeader == null || companyHeader.isBlank()) {
            companyHeader = request.getHeader(COMPANY_ID_LEGACY_HEADER);
        }
        if (companyHeader != null && !companyHeader.isBlank()) {
            return parseUuid(companyHeader, "CompanyId header must be valid UUID");
        }
        Object companyIdClaim = jwt.getClaims().get("companyId");
        if (companyIdClaim instanceof String companyId && !companyId.isBlank()) {
            return parseUuid(companyId, "companyId claim must be valid UUID");
        }
        throw new IllegalArgumentException("CompanyId is required");
    }

    private UUID parseUuid(String value, String message) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(message);
        }
    }

    private void writeErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse(message)));
    }
}
