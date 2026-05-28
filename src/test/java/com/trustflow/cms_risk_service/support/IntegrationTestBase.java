package com.trustflow.cms_risk_service.support;

import com.trustflow.cms_risk_service.core.rule.application.port.out.PermissionCheckPort;
import com.trustflow.cms_risk_service.core.security.Permission;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestBase.KafkaStubConfiguration.class)
public abstract class IntegrationTestBase {
    protected static final UUID TEST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    protected static final UUID TEST_COMPANY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final EmbeddedPostgres EMBEDDED_POSTGRES = startEmbeddedPostgres();

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected PermissionCheckPort permissionCheckPort;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> EMBEDDED_POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("app.security.jwt.public-key", TestJwtSupport::publicKeyPem);
    }

    @BeforeEach
    void grantManagePermissionByDefault() {
        when(permissionCheckPort.hasPermission(any(), any(), eq(Permission.MANAGE_RULES_AND_RISKS)))
                .thenReturn(true);
    }

    protected String authToken() {
        return TestJwtSupport.bearerToken(TEST_USER_ID, TEST_COMPANY_ID);
    }

    private static EmbeddedPostgres startEmbeddedPostgres() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start embedded PostgreSQL for integration tests", exception);
        }
    }

    @TestConfiguration
    static class KafkaStubConfiguration {
        @Bean
        KafkaTemplate<String, String> kafkaTemplate() {
            return mock(KafkaTemplate.class);
        }
    }
}
