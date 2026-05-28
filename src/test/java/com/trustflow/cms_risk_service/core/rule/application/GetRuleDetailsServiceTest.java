package com.trustflow.cms_risk_service.core.rule.application;

import com.trustflow.cms_risk_service.core.common.exception.NotFoundException;
import com.trustflow.cms_risk_service.core.rule.application.port.out.RuleQueryRepository;
import com.trustflow.cms_risk_service.core.rule.domain.Rule;
import com.trustflow.cms_risk_service.core.rule.domain.RuleAction;
import com.trustflow.cms_risk_service.core.rule.domain.RulePriority;
import com.trustflow.cms_risk_service.core.security.UserContext;
import com.trustflow.cms_risk_service.core.security.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetRuleDetailsServiceTest {
    private static final UUID RULE_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID COMPANY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID CATEGORY_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private RuleQueryRepository ruleQueryRepository;

    @InjectMocks
    private GetRuleDetailsService getRuleDetailsService;

    @BeforeEach
    void setUp() {
        UserContextHolder.set(new UserContext(USER_ID, COMPANY_ID, "token", USER_ID.toString()));
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void getRuleDetails_throwsNotFoundForAnotherCompanyRule() {
        when(ruleQueryRepository.findByIdAndCompanyId(RULE_ID, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getRuleDetailsService.getRuleDetails(RULE_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Правило не найдено");
    }

    @Test
    void getRuleDetails_returnsMappedFields() {
        Rule rule = new Rule(
                RULE_ID,
                COMPANY_ID,
                "Rule A",
                "amount > 1",
                CATEGORY_ID,
                null,
                RulePriority.MEDIUM,
                USER_ID,
                List.of(RuleAction.CREATE_INCIDENT),
                true,
                "script.groovy",
                "return true",
                USER_ID,
                Instant.parse("2026-05-18T08:00:00Z")
        );
        when(ruleQueryRepository.findByIdAndCompanyId(RULE_ID, COMPANY_ID)).thenReturn(Optional.of(rule));

        RuleDetailsResult result = getRuleDetailsService.getRuleDetails(RULE_ID);

        assertThat(result.id()).isEqualTo(RULE_ID);
        assertThat(result.name()).isEqualTo("Rule A");
        assertThat(result.condition()).isEqualTo("amount > 1");
        assertThat(result.priority()).isEqualTo(RulePriority.MEDIUM);
        assertThat(result.mechanismScriptContent()).isEqualTo("return true");
    }
}
