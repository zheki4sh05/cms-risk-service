package com.trustflow.cms_risk_service.infrastructure.monitoring;

import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.RuleJpaEntity;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.RuleJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class RuleExecutionStatsService {
    private final RuleJpaRepository ruleJpaRepository;
    private final ConcurrentMap<UUID, ReentrantLock> ruleLocks = new ConcurrentHashMap<>();

    @Transactional
    public void registerSuccess(UUID ruleId) {
        withRuleLock(ruleId, () -> {
            RuleJpaEntity entity = loadRule(ruleId);
            entity.setSuccessCount(entity.getSuccessCount() + 1);
            entity.setLastDateInvocation(Instant.now());
            ruleJpaRepository.save(entity);
        });
    }

    @Transactional
    public void registerTrigger(UUID ruleId) {
        withRuleLock(ruleId, () -> {
            RuleJpaEntity entity = loadRule(ruleId);
            entity.setTriggersCount(entity.getTriggersCount() + 1);
            entity.setLastDateInvocation(Instant.now());
            entity.setLastDateTrigger(LocalDate.now());
            ruleJpaRepository.save(entity);
        });
    }

    @Transactional
    public void registerFailure(UUID ruleId) {
        withRuleLock(ruleId, () -> {
            RuleJpaEntity entity = loadRule(ruleId);
            entity.setFailedCount(entity.getFailedCount() + 1);
            entity.setLastDateInvocation(Instant.now());
            ruleJpaRepository.save(entity);
        });
    }

    private RuleJpaEntity loadRule(UUID ruleId) {
        return ruleJpaRepository.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Rule not found: " + ruleId));
    }

    private void withRuleLock(UUID ruleId, Runnable action) {
        ReentrantLock lock = ruleLocks.computeIfAbsent(ruleId, id -> new ReentrantLock());
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                ruleLocks.remove(ruleId, lock);
            }
        }
    }
}
