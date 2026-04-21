package com.trustflow.cms_risk_service.infrastructure.persistence;

import com.trustflow.cms_risk_service.infrastructure.config.RuleHistoryCleanupProperties;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.RuleHistoryJpaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleHistoryCleanupScheduler {
    private final RuleHistoryJpaRepository ruleHistoryJpaRepository;
    private final RuleHistoryCleanupProperties cleanupProperties;
    private final Clock clock;

    @Scheduled(cron = "${app.rules-history.cleanup.cron}")
    @Transactional
    public void cleanupOldRecords() {
        long retentionDays = cleanupProperties.retentionDays();
        if (retentionDays <= 0) {
            log.warn("Skip rules_history cleanup: retentionDays must be positive, current={}", retentionDays);
            return;
        }

        Instant threshold = Instant.now(clock).minus(retentionDays, ChronoUnit.DAYS);
        long deleted = ruleHistoryJpaRepository.deleteByChangedAtBefore(threshold);
        log.debug("rules_history cleanup deleted={} threshold={}", deleted, threshold);
    }
}
