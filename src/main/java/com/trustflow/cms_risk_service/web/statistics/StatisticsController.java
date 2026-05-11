package com.trustflow.cms_risk_service.web.statistics;

import com.trustflow.cms_risk_service.core.security.UserContextHolder;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.OutbooxMonitoringJpaRepository;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.VerificationResultJpaRepository;
import com.trustflow.cms_risk_service.web.statistics.dto.StatisticsResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final OutbooxMonitoringJpaRepository outbooxMonitoringJpaRepository;
    private final VerificationResultJpaRepository verificationResultJpaRepository;

    @Operation(
            summary = "Returns row counts: outbox monitoring queue (global) and verification results for the current company."
    )
    @GetMapping
    public StatisticsResponse get() {
        UUID companyId = UserContextHolder.getRequired().companyId();
        long outboxCount = outbooxMonitoringJpaRepository.count();
        long verificationResultCount = verificationResultJpaRepository.countByCompanyId(companyId);
        return new StatisticsResponse(outboxCount, verificationResultCount);
    }
}
