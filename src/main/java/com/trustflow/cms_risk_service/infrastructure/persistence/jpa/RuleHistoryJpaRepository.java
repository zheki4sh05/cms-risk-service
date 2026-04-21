package com.trustflow.cms_risk_service.infrastructure.persistence.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RuleHistoryJpaRepository extends JpaRepository<RuleHistoryJpaEntity, UUID> {
    @Query(
            value = "SELECT * " +
                    "FROM rules_history history " +
                    "WHERE history.company_id = :companyId " +
                    "  AND ( " +
                    "    :query IS NULL " +
                    "    OR CAST(history.rule_name AS TEXT) ILIKE CONCAT('%', CAST(:query AS TEXT), '%') " +
                    "    OR CAST(history.description AS TEXT) ILIKE CONCAT('%', CAST(:query AS TEXT), '%') " +
                    "  ) " +
                    "ORDER BY history.changed_at DESC, history.id DESC",
            nativeQuery = true
    )
    Slice<RuleHistoryJpaEntity> findChangeHistory(
            @Param("companyId") UUID companyId,
            @Param("query") String query,
            Pageable pageable
    );

    Optional<RuleHistoryJpaEntity> findByIdAndCompanyId(UUID id, UUID companyId);

    @Modifying
    long deleteByChangedAtBefore(Instant threshold);
}
