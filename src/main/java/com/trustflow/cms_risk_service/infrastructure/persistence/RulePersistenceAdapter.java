package com.trustflow.cms_risk_service.infrastructure.persistence;

import com.trustflow.cms_risk_service.core.rule.application.port.out.RuleCommandRepository;
import com.trustflow.cms_risk_service.core.rule.application.port.out.RuleQueryRepository;
import com.trustflow.cms_risk_service.core.rule.domain.Rule;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.RuleJpaEntity;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.RuleJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class RulePersistenceAdapter implements RuleCommandRepository, RuleQueryRepository {
    private final RuleJpaRepository ruleJpaRepository;
    private final RulePersistenceMapper rulePersistenceMapper;

    public RulePersistenceAdapter(RuleJpaRepository ruleJpaRepository, RulePersistenceMapper rulePersistenceMapper) {
        this.ruleJpaRepository = ruleJpaRepository;
        this.rulePersistenceMapper = rulePersistenceMapper;
    }

    @Override
    public Rule save(Rule rule) {
        RuleJpaEntity entity = rulePersistenceMapper.toEntity(rule);
        RuleJpaEntity saved = ruleJpaRepository.save(entity);
        return rulePersistenceMapper.toDomain(saved);
    }

    @Override
    public List<Rule> findAllByCompanyId(UUID companyId) {
        return ruleJpaRepository.findAllByCompanyIdOrderBySavedAtDesc(companyId)
                .stream()
                .map(rulePersistenceMapper::toDomain)
                .toList();
    }
}
