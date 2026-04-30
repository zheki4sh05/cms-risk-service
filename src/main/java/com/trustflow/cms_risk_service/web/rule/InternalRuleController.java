package com.trustflow.cms_risk_service.web.rule;

import com.trustflow.cms_risk_service.core.common.exception.ForbiddenException;
import com.trustflow.cms_risk_service.core.common.exception.NotFoundException;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.RuleJpaEntity;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.RuleJpaRepository;
import com.trustflow.cms_risk_service.web.rule.dto.InternalRuleResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/rules")
@RequiredArgsConstructor
public class InternalRuleController {
    private final RuleJpaRepository ruleJpaRepository;

    @Operation(summary = "Returns rule details for internal localhost-only integrations.")
    @GetMapping("/{id}")
    public InternalRuleResponse getById(
            @PathVariable("id") UUID ruleId,
            HttpServletRequest request
    ) {
        validateRequestIsFromLocalhost(request);
        RuleJpaEntity rule = ruleJpaRepository.findById(ruleId)
                .orElseThrow(() -> new NotFoundException("Rule not found: " + ruleId));
        return toResponse(rule);
    }

    private void validateRequestIsFromLocalhost(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        boolean localhost = "127.0.0.1".equals(remoteAddress)
                || "0:0:0:0:0:0:0:1".equals(remoteAddress)
                || "::1".equals(remoteAddress)
                || "localhost".equalsIgnoreCase(remoteAddress);
        if (!localhost) {
            throw new ForbiddenException("Internal endpoint is available only from localhost");
        }
    }

    private InternalRuleResponse toResponse(RuleJpaEntity rule) {
        return new InternalRuleResponse(
                rule.getId(),
                rule.getCompanyId(),
                rule.getName(),
                rule.getCondition(),
                rule.getCategoryId(),
                rule.getRiskObjectId(),
                rule.getPriority(),
                rule.getResponsibleUserId(),
                rule.getActions(),
                rule.isEnabled(),
                rule.getMechanismScriptName(),
                rule.getMechanismScriptContent(),
                rule.getCreatedByUserId(),
                rule.getSavedAt(),
                rule.getSuccessCount(),
                rule.getTriggersCount(),
                rule.getFailedCount(),
                rule.getLastDateInvocation(),
                rule.getLastDateTrigger()
        );
    }
}
