package com.trustflow.cms_risk_service.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationResultJpaRepository extends JpaRepository<VerificationResultJpaEntity, UUID> {
    List<VerificationResultJpaEntity> findAllByCompanyIdOrderByIdDesc(UUID companyId);

    Optional<VerificationResultJpaEntity> findByIdAndCompanyId(UUID id, UUID companyId);

    long countByCompanyId(UUID companyId);
}
