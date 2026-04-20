package com.trustflow.cms_risk_service.infrastructure.persistence;

import com.trustflow.cms_risk_service.core.riskcategory.application.port.out.RiskCategoryRepository;
import com.trustflow.cms_risk_service.core.riskcategory.domain.RiskCategory;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.RiskCategoryJpaEntity;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.RiskCategoryJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RiskCategoryPersistenceAdapter implements RiskCategoryRepository {
    private final RiskCategoryJpaRepository riskCategoryJpaRepository;
    private final RiskCategoryPersistenceMapper riskCategoryPersistenceMapper;

    public RiskCategoryPersistenceAdapter(
            RiskCategoryJpaRepository riskCategoryJpaRepository,
            RiskCategoryPersistenceMapper riskCategoryPersistenceMapper
    ) {
        this.riskCategoryJpaRepository = riskCategoryJpaRepository;
        this.riskCategoryPersistenceMapper = riskCategoryPersistenceMapper;
    }

    @Override
    public List<RiskCategory> findAllByCompanyId(UUID companyId) {
        return riskCategoryJpaRepository.findAllByCompanyIdOrderByNameAsc(companyId)
                .stream()
                .map(riskCategoryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<RiskCategory> findByIdAndCompanyId(UUID id, UUID companyId) {
        return riskCategoryJpaRepository.findByIdAndCompanyId(id, companyId)
                .map(riskCategoryPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByCompanyIdAndNameIgnoreCase(UUID companyId, String name) {
        return riskCategoryJpaRepository.existsByCompanyIdAndNameIgnoreCase(companyId, name);
    }

    @Override
    public boolean existsByCompanyIdAndNameIgnoreCaseAndIdNot(UUID companyId, String name, UUID categoryId) {
        return riskCategoryJpaRepository.existsByCompanyIdAndNameIgnoreCaseAndIdNot(companyId, name, categoryId);
    }

    @Override
    public RiskCategory save(RiskCategory category) {
        RiskCategoryJpaEntity entity = riskCategoryPersistenceMapper.toEntity(category);
        RiskCategoryJpaEntity saved = riskCategoryJpaRepository.save(entity);
        return riskCategoryPersistenceMapper.toDomain(saved);
    }

    @Override
    public void delete(RiskCategory category) {
        riskCategoryJpaRepository.deleteById(category.id());
    }
}
