package com.trustflow.cms_risk_service.web.riskcategory;

import com.trustflow.cms_risk_service.core.riskcategory.application.CreateRiskCategoryCommand;
import com.trustflow.cms_risk_service.core.riskcategory.application.ListRiskCategoriesResult;
import com.trustflow.cms_risk_service.core.riskcategory.application.RiskCategoryResult;
import com.trustflow.cms_risk_service.core.riskcategory.application.UpdateRiskCategoryCommand;
import com.trustflow.cms_risk_service.web.riskcategory.dto.ListRiskCategoriesResponse;
import com.trustflow.cms_risk_service.web.riskcategory.dto.RiskCategoryResponse;
import com.trustflow.cms_risk_service.web.riskcategory.dto.UpsertRiskCategoryRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RiskCategoryWebMapper {
    CreateRiskCategoryCommand toCreateCommand(UpsertRiskCategoryRequest request);

    UpdateRiskCategoryCommand toUpdateCommand(UpsertRiskCategoryRequest request);

    RiskCategoryResponse toResponse(RiskCategoryResult result);

    ListRiskCategoriesResponse toListResponse(ListRiskCategoriesResult result);
}
