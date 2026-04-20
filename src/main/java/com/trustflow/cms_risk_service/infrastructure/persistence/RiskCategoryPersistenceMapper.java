package com.trustflow.cms_risk_service.infrastructure.persistence;

import com.trustflow.cms_risk_service.core.riskcategory.domain.RiskCategory;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.RiskCategoryJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RiskCategoryPersistenceMapper {
    RiskCategoryJpaEntity toEntity(RiskCategory category);

    RiskCategory toDomain(RiskCategoryJpaEntity entity);
}
