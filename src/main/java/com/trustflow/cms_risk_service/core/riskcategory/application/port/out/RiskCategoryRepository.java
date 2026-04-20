package com.trustflow.cms_risk_service.core.riskcategory.application.port.out;

import com.trustflow.cms_risk_service.core.riskcategory.domain.RiskCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskCategoryRepository {
    List<RiskCategory> findAllByCompanyId(UUID companyId);

    Optional<RiskCategory> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCompanyIdAndNameIgnoreCase(UUID companyId, String name);

    boolean existsByCompanyIdAndNameIgnoreCaseAndIdNot(UUID companyId, String name, UUID categoryId);

    RiskCategory save(RiskCategory category);

    void delete(RiskCategory category);
}
