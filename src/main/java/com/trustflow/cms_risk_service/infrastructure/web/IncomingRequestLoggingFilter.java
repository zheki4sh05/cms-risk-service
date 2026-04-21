package com.trustflow.cms_risk_service.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class IncomingRequestLoggingFilter extends OncePerRequestFilter {
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
        long startedAtMs = System.currentTimeMillis();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String fullPath = query == null ? path : path + "?" + query;

        log.debug("Incoming request: method={} path={}", method, fullPath);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAtMs;
            log.debug(
                    "Completed request: method={} path={} status={} durationMs={}",
                    method,
                    fullPath,
                    response.getStatus(),
                    durationMs
            );
        }
    }
}
