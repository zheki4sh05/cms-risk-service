package com.trustflow.cms_risk_service.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskCategoryJpaRepository extends JpaRepository<RiskCategoryJpaEntity, UUID> {
    List<RiskCategoryJpaEntity> findAllByCompanyIdOrderByNameAsc(UUID companyId);

    Optional<RiskCategoryJpaEntity> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCompanyIdAndNameIgnoreCase(UUID companyId, String name);

    boolean existsByCompanyIdAndNameIgnoreCaseAndIdNot(UUID companyId, String name, UUID id);
}
