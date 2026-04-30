package com.trustflow.cms_risk_service.web.riskcategory;

import com.trustflow.cms_risk_service.core.riskcategory.application.CreateRiskCategoryCommand;
import com.trustflow.cms_risk_service.core.riskcategory.application.ListRiskCategoriesResult;
import com.trustflow.cms_risk_service.core.riskcategory.application.RiskCategoryResult;
import com.trustflow.cms_risk_service.core.riskcategory.application.UpdateRiskCategoryCommand;
import com.trustflow.cms_risk_service.core.riskcategory.application.port.in.ManageRiskCategoriesUseCase;
import com.trustflow.cms_risk_service.web.riskcategory.dto.ListRiskCategoriesResponse;
import com.trustflow.cms_risk_service.web.riskcategory.dto.RiskCategoryResponse;
import com.trustflow.cms_risk_service.web.riskcategory.dto.UpsertRiskCategoryRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/risk-categories")
@RequiredArgsConstructor
public class RiskCategoryController {
    private final ManageRiskCategoriesUseCase manageRiskCategoriesUseCase;
    private final RiskCategoryWebMapper riskCategoryWebMapper;

    @Operation(summary = "Returns all available risk categories.")
    @GetMapping
    public ListRiskCategoriesResponse listCategories() {
        ListRiskCategoriesResult result = manageRiskCategoriesUseCase.listCategories();
        return riskCategoryWebMapper.toListResponse(result);
    }

    @Operation(summary = "Creates a new risk category from the provided payload.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RiskCategoryResponse createCategory(@Valid @RequestBody UpsertRiskCategoryRequest request) {
        CreateRiskCategoryCommand command = riskCategoryWebMapper.toCreateCommand(request);
        RiskCategoryResult result = manageRiskCategoriesUseCase.createCategory(command);
        return riskCategoryWebMapper.toResponse(result);
    }

    @Operation(summary = "Updates an existing risk category by its ID.")
    @PutMapping("/{id}")
    public RiskCategoryResponse updateCategory(
            @PathVariable("id") UUID categoryId,
            @Valid @RequestBody UpsertRiskCategoryRequest request
    ) {
        UpdateRiskCategoryCommand command = riskCategoryWebMapper.toUpdateCommand(request);
        RiskCategoryResult result = manageRiskCategoriesUseCase.updateCategory(categoryId, command);
        return riskCategoryWebMapper.toResponse(result);
    }

    @Operation(summary = "Deletes a risk category by its ID.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable("id") UUID categoryId) {
        manageRiskCategoriesUseCase.deleteCategory(categoryId);
    }
}
