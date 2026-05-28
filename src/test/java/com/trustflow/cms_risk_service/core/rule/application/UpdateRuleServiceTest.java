package com.trustflow.cms_risk_service.core.rule.application;

import com.trustflow.cms_risk_service.core.common.exception.ForbiddenException;
import com.trustflow.cms_risk_service.core.common.exception.NotFoundException;
import com.trustflow.cms_risk_service.core.rule.application.port.out.PermissionCheckPort;
import com.trustflow.cms_risk_service.core.rule.application.port.out.RuleCommandRepository;
import com.trustflow.cms_risk_service.core.rule.application.port.out.RuleQueryRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateRuleServiceTest {
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-18T12:00:00Z");
    private static final UUID RULE_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID COMPANY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID CATEGORY_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private RuleCommandRepository ruleCommandRepository;
    @Mock
    private RuleQueryRepository ruleQueryRepository;
    @Mock
    private PermissionCheckPort permissionCheckPort;

    @InjectMocks
    private UpdateRuleService updateRuleService;

    private final Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        updateRuleService = new UpdateRuleService(
                ruleCommandRepository,
                ruleQueryRepository,
                permissionCheckPort,
                clock
        );
        UserContextHolder.set(new UserContext(USER_ID, COMPANY_ID, "token", USER_ID.toString()));
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void updateRule_throwsNotFoundWhenRuleMissing() {
        when(permissionCheckPort.hasPermission(eq(USER_ID), eq("token"), eq(Permission.MANAGE_RULES_AND_RISKS)))
                .thenReturn(true);
        when(ruleQueryRepository.findByIdAndCompanyId(RULE_ID, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateRuleService.updateRule(sampleCommand()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Правило не найдено");
    }

    @Test
    void updateRule_throwsForbiddenWhenPermissionMissing() {
        when(permissionCheckPort.hasPermission(eq(USER_ID), eq("token"), eq(Permission.MANAGE_RULES_AND_RISKS)))
                .thenReturn(false);

        assertThatThrownBy(() -> updateRuleService.updateRule(sampleCommand()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateRule_savesUpdatedRuleAndHistory() {
        when(permissionCheckPort.hasPermission(eq(USER_ID), eq("token"), eq(Permission.MANAGE_RULES_AND_RISKS)))
                .thenReturn(true);

        Rule existingRule = new Rule(
                RULE_ID,
                COMPANY_ID,
                "Old name",
                "old > 1",
                CATEGORY_ID,
                null,
                RulePriority.LOW,
                USER_ID,
                List.of(RuleAction.SEND_NOTIFICATION),
                false,
                null,
                null,
                USER_ID,
                Instant.parse("2026-05-17T10:00:00Z")
        );
        when(ruleQueryRepository.findByIdAndCompanyId(RULE_ID, COMPANY_ID)).thenReturn(Optional.of(existingRule));

        UpdateRuleCommand command = sampleCommand();
        ArgumentCaptor<Rule> ruleCaptor = ArgumentCaptor.forClass(Rule.class);
        when(ruleCommandRepository.save(any(Rule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateRuleResult result = updateRuleService.updateRule(command);

        verify(ruleCommandRepository).save(ruleCaptor.capture());
        Rule savedRule = ruleCaptor.getValue();
        assertThat(savedRule.name()).isEqualTo("Updated rule");
        assertThat(savedRule.condition()).isEqualTo("amount > 5000");
        assertThat(savedRule.priority()).isEqualTo(RulePriority.HIGH);
        assertThat(savedRule.savedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(result.id()).isEqualTo(RULE_ID);
        verify(ruleCommandRepository).saveHistory(any(RuleHistoryWriteCommand.class));
    }

    private UpdateRuleCommand sampleCommand() {
        return new UpdateRuleCommand(
                RULE_ID,
                "Changed threshold",
                "Updated rule",
                "amount > 5000",
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
