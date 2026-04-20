package com.trustflow.cms_risk_service.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RuleJpaRepository extends JpaRepository<RuleJpaEntity, UUID> {
    List<RuleJpaEntity> findAllByCompanyIdOrderBySavedAtDesc(UUID companyId);
}
