package com.trustflow.cms_risk_service.infrastructure.persistence;

import com.trustflow.cms_risk_service.core.rule.application.RuleChangeHistoryEntry;
import com.trustflow.cms_risk_service.core.rule.application.RuleChangeHistoryDetailsResult;
import com.trustflow.cms_risk_service.core.rule.application.RuleChangeHistoryPageResult;
import com.trustflow.cms_risk_service.core.rule.application.RuleHistoryWriteCommand;
import com.trustflow.cms_risk_service.core.rule.application.port.out.RuleCommandRepository;
import com.trustflow.cms_risk_service.core.rule.application.port.out.RuleQueryRepository;
import com.trustflow.cms_risk_service.core.rule.domain.Rule;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.RuleHistoryJpaRepository;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.RuleHistoryJpaEntity;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.RuleJpaEntity;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.RuleJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RulePersistenceAdapter implements RuleCommandRepository, RuleQueryRepository {
    private final RuleJpaRepository ruleJpaRepository;
    private final RuleHistoryJpaRepository ruleHistoryJpaRepository;
    private final RulePersistenceMapper rulePersistenceMapper;

    public RulePersistenceAdapter(
            RuleJpaRepository ruleJpaRepository,
            RuleHistoryJpaRepository ruleHistoryJpaRepository,
            RulePersistenceMapper rulePersistenceMapper
    ) {
        this.ruleJpaRepository = ruleJpaRepository;
        this.ruleHistoryJpaRepository = ruleHistoryJpaRepository;
        this.rulePersistenceMapper = rulePersistenceMapper;
    }

    @Override
    public Rule save(Rule rule) {
        RuleJpaEntity entity = rulePersistenceMapper.toEntity(rule);
        if (rule.id() != null) {
            ruleJpaRepository.findById(rule.id()).ifPresent(existing -> {
                entity.setSuccessCount(existing.getSuccessCount());
                entity.setTriggersCount(existing.getTriggersCount());
                entity.setFailedCount(existing.getFailedCount());
                entity.setLastDateInvocation(existing.getLastDateInvocation());
                entity.setLastDateTrigger(existing.getLastDateTrigger());
            });
        }
        RuleJpaEntity saved = ruleJpaRepository.save(entity);
        return rulePersistenceMapper.toDomain(saved);
    }

    @Override
    public void saveHistory(RuleHistoryWriteCommand command) {
        RuleHistoryJpaEntity entity = new RuleHistoryJpaEntity();
        entity.setId(command.id());
        entity.setCompanyId(command.companyId());
        entity.setRuleId(command.ruleId());
        entity.setRuleName(command.ruleName());
        entity.setDescription(command.description());
        entity.setAuthorId(command.authorId());
        entity.setCondition(command.condition());
        entity.setCategoryId(command.categoryId());
        entity.setRiskObjectId(command.riskObjectId());
        entity.setPriority(command.priority());
        entity.setResponsibleUserId(command.responsibleUserId());
        entity.setActions(command.actions() == null
                ? null
                : command.actions().stream().map(action -> action.value()).toList());
        entity.setEnabled(command.enabled());
        entity.setMechanismScriptName(command.mechanismScriptName());
        entity.setMechanismScriptContent(command.mechanismScriptContent());
        entity.setCreatedByUserId(command.createdByUserId());
        entity.setSavedAt(command.savedAt());
        entity.setChangedAt(command.changedAt());
        ruleHistoryJpaRepository.save(entity);
    }

    @Override
    public List<Rule> findAllByCompanyId(UUID companyId) {
        return ruleJpaRepository.findAllByCompanyIdOrderBySavedAtDesc(companyId)
                .stream()
                .map(rulePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Rule> findByIdAndCompanyId(UUID id, UUID companyId) {
        return ruleJpaRepository.findByIdAndCompanyId(id, companyId)
                .map(rulePersistenceMapper::toDomain);
    }

    @Override
    public RuleChangeHistoryPageResult findChangeHistoryByCompanyId(UUID companyId, String query, int page, int pageSize) {
        var historySlice = ruleHistoryJpaRepository.findChangeHistory(
                companyId,
                query,
                PageRequest.of(page - 1, pageSize)
        );
        return new RuleChangeHistoryPageResult(
                historySlice.getContent().stream()
                        .map(item -> new RuleChangeHistoryEntry(
                                item.getId(),
                                item.getRuleId(),
                                item.getChangedAt(),
                                item.getRuleName(),
                                item.getDescription(),
                                item.getAuthorId()
                        ))
                        .toList(),
                historySlice.hasNext()
        );
    }

    @Override
    public Optional<RuleChangeHistoryDetailsResult> findChangeHistoryDetailsByIdAndCompanyId(UUID id, UUID companyId) {
        return ruleHistoryJpaRepository.findByIdAndCompanyId(id, companyId)
                .map(item -> new RuleChangeHistoryDetailsResult(
                        item.getId(),
                        item.getCompanyId(),
                        item.getRuleId(),
                        item.getRuleName(),
                        item.getDescription(),
                        item.getAuthorId(),
                        null,
                        item.getCondition(),
                        item.getCategoryId(),
                        item.getRiskObjectId(),
                        item.getPriority(),
                        item.getResponsibleUserId(),
                        item.getActions(),
                        item.getEnabled(),
                        item.getMechanismScriptName(),
                        item.getMechanismScriptContent(),
                        item.getCreatedByUserId(),
                        item.getSavedAt(),
                        item.getChangedAt()
                ));
    }
}
