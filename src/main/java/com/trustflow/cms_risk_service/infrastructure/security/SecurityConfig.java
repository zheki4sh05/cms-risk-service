package com.trustflow.cms_risk_service.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustflow.cms_risk_service.web.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    private final ObjectMapper objectMapper;
    private final UserContextFilter userContextFilter;

    public SecurityConfig(ObjectMapper objectMapper, UserContextFilter userContextFilter) {
        this.objectMapper = objectMapper;
        this.userContextFilter = userContextFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(registry -> registry
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/rules").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/rules").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/risk-categories").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/risk-categories").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/risk-categories/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/risk-categories/*").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(configurer -> configurer
                        .authenticationEntryPoint((request, response, authException) ->
                                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden")))
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .addFilterAfter(userContextFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }

    private void writeErrorResponse(HttpServletResponse response, int statusCode, String message) throws java.io.IOException {
        response.setStatus(statusCode);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse(message)));
    }
}
