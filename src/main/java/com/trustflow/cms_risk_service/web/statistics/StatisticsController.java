package com.trustflow.cms_risk_service.web.statistics;

import com.trustflow.cms_risk_service.core.security.UserContextHolder;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.OutbooxMonitoringJpaRepository;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.VerificationResultJpaRepository;
import com.trustflow.cms_risk_service.web.statistics.dto.StatisticsResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class StatisticsController {
    private final OutbooxMonitoringJpaRepository outbooxMonitoringJpaRepository;
    private final VerificationResultJpaRepository verificationResultJpaRepository;

    @Operation(
            summary = "Returns row counts: outbox monitoring queue (global) and verification results for the current company."
    )
    @GetMapping({"/api/rules/processing/statistic", "/api/risks/processing/statistic"})
    public StatisticsResponse get(HttpServletRequest request) {
        String uri = request.getRequestURI();
        log.info("GET {}: request received", uri);
        try {
            UUID companyId = UserContextHolder.getRequired().companyId();
            log.info("GET {}: resolved companyId={}", uri, companyId);

            long outboxCount = outbooxMonitoringJpaRepository.count();
            log.debug("GET {}: outboxCount={}", uri, outboxCount);

            long verificationResultCount = verificationResultJpaRepository.countByCompanyId(companyId);
            log.debug("GET {}: verificationResultCount={}", uri, verificationResultCount);

            StatisticsResponse response = new StatisticsResponse(outboxCount, verificationResultCount);
            log.info(
                    "GET {}: success companyId={} outboxCount={} verificationResultCount={}",
                    uri,
                    companyId,
                    outboxCount,
                    verificationResultCount
            );
            return response;
        } catch (Exception exception) {
            log.error("GET {}: failed", uri, exception);
            throw exception;
        }
    }
}
