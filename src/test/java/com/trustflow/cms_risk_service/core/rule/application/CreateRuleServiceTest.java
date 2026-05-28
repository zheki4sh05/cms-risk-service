package com.trustflow.cms_risk_service.core.rule.application;

import com.trustflow.cms_risk_service.core.common.exception.ForbiddenException;
import com.trustflow.cms_risk_service.core.rule.application.port.out.PermissionCheckPort;
import com.trustflow.cms_risk_service.core.rule.application.port.out.RuleCommandRepository;
import com.trustflow.cms_risk_service.core.rule.domain.Rule;
import com.trustflow.cms_risk_service.core.rule.domain.RuleAction;
import com.trustflow.cms_risk_service.core.rule.domain.RulePriority;
import com.trustflow.cms_risk_service.core.security.Permission;
import com.trustflow.cms_risk_service.core.security.UserContext;
import com.trustflow.cms_risk_service.core.security.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateRuleServiceTest {
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-18T10:00:00Z");
    private static final UUID COMPANY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID CATEGORY_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private RuleCommandRepository ruleCommandRepository;
    @Mock
    private PermissionCheckPort permissionCheckPort;
    @Mock
    private RuleDomainMapper ruleDomainMapper;

    @InjectMocks
    private CreateRuleService createRuleService;

    private final Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        createRuleService = new CreateRuleService(ruleCommandRepository, permissionCheckPort, ruleDomainMapper, clock);
        UserContextHolder.set(new UserContext(USER_ID, COMPANY_ID, "token", USER_ID.toString()));
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void createRule_throwsForbiddenWhenPermissionMissing() {
        when(permissionCheckPort.hasPermission(eq(USER_ID), eq("token"), eq(Permission.MANAGE_RULES_AND_RISKS)))
                .thenReturn(false);

        CreateRuleCommand command = sampleCommand();

        assertThatThrownBy(() -> createRuleService.createRule(command))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining(Permission.MANAGE_RULES_AND_RISKS.value());
    }

    @Test
    void createRule_persistsRuleWhenPermissionGranted() {
        when(permissionCheckPort.hasPermission(eq(USER_ID), eq("token"), eq(Permission.MANAGE_RULES_AND_RISKS)))
                .thenReturn(true);

        CreateRuleCommand command = sampleCommand();
        UUID ruleId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        Rule mappedRule = new Rule(
                ruleId,
                COMPANY_ID,
                command.name(),
                command.condition(),
                command.categoryId(),
                command.riskObjectId(),
                command.priority(),
                command.responsibleUserId(),
                command.actions(),
                command.enabled(),
                command.mechanismScriptName(),
                command.mechanismScriptContent(),
                USER_ID,
                FIXED_INSTANT
        );
        when(ruleDomainMapper.toRule(eq(command), any(UUID.class), eq(COMPANY_ID), eq(USER_ID), eq(FIXED_INSTANT)))
                .thenReturn(mappedRule);
        when(ruleCommandRepository.save(mappedRule)).thenReturn(mappedRule);

        CreateRuleResult result = createRuleService.createRule(command);

        assertThat(result.id()).isEqualTo(ruleId);
        assertThat(result.savedAt()).isEqualTo(FIXED_INSTANT);
        verify(ruleCommandRepository).save(mappedRule);
    }

    private CreateRuleCommand sampleCommand() {
        return new CreateRuleCommand(
                "High amount transfer",
                "amount > 10000",
                CATEGORY_ID,
                null,
                RulePriority.HIGH,
                USER_ID,
                List.of(RuleAction.CREATE_INCIDENT),
                true,
                null,
                null
        );
    }
}
