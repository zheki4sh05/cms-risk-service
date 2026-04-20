package com.trustflow.cms_risk_service.infrastructure.config;

import com.trustflow.cms_risk_service.infrastructure.auth.AuthServiceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AuthServiceProperties.class)
public class HttpClientsConfig {
    @Bean
    public RestClient authServiceRestClient(RestClient.Builder builder, AuthServiceProperties properties) {
        return builder
                .baseUrl(properties.baseUrl())
                .build();
    }
}
