package com.trustflow.cms_risk_service.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutbooxMonitoringJpaRepository extends JpaRepository<OutbooxMonitoringJpaEntity, UUID> {
    List<OutbooxMonitoringJpaEntity> findTop50ByOrderByIdAsc();
}
