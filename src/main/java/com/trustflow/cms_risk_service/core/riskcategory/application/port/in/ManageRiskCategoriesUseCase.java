package com.trustflow.cms_risk_service.core.riskcategory.application.port.in;

import com.trustflow.cms_risk_service.core.riskcategory.application.CreateRiskCategoryCommand;
import com.trustflow.cms_risk_service.core.riskcategory.application.ListRiskCategoriesResult;
import com.trustflow.cms_risk_service.core.riskcategory.application.RiskCategoryResult;
import com.trustflow.cms_risk_service.core.riskcategory.application.UpdateRiskCategoryCommand;

import java.util.UUID;

public interface ManageRiskCategoriesUseCase {
    ListRiskCategoriesResult listCategories();

    RiskCategoryResult createCategory(CreateRiskCategoryCommand command);

    RiskCategoryResult updateCategory(UUID categoryId, UpdateRiskCategoryCommand command);

    void deleteCategory(UUID categoryId);
}
