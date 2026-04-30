package com.trustflow.cms_risk_service.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustflow.cms_risk_service.infrastructure.kafka.KafkaTopicProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({
        RuleHistoryCleanupProperties.class,
        KafkaTopicProperties.class
})
public class ApplicationBeansConfig {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
