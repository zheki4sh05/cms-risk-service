package com.trustflow.cms_risk_service.core.riskcategory.application;

import com.trustflow.cms_risk_service.core.common.exception.ConflictException;
import com.trustflow.cms_risk_service.core.common.exception.ForbiddenException;
import com.trustflow.cms_risk_service.core.common.exception.NotFoundException;
import com.trustflow.cms_risk_service.core.riskcategory.application.port.in.ManageRiskCategoriesUseCase;
import com.trustflow.cms_risk_service.core.riskcategory.application.port.out.RiskCategoryRepository;
import com.trustflow.cms_risk_service.core.rule.application.port.out.PermissionCheckPort;
import com.trustflow.cms_risk_service.core.riskcategory.domain.RiskCategory;
import com.trustflow.cms_risk_service.core.security.Permission;
import com.trustflow.cms_risk_service.core.security.UserContext;
import com.trustflow.cms_risk_service.core.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManageRiskCategoriesService implements ManageRiskCategoriesUseCase {
    private static final String DUPLICATE_NAME_MESSAGE = "Категория с таким названием уже существует";
    private static final String CATEGORY_NOT_FOUND_MESSAGE = "Категория не найдена";

    private final RiskCategoryRepository riskCategoryRepository;
    private final PermissionCheckPort permissionCheckPort;

    @Override
    public ListRiskCategoriesResult listCategories() {
        UserContext userContext = UserContextHolder.getRequired();
        List<RiskCategoryResult> items = riskCategoryRepository.findAllByCompanyId(userContext.companyId())
                .stream()
                .map(category -> new RiskCategoryResult(category.id(), category.name()))
                .toList();
        return new ListRiskCategoriesResult(items);
    }

    @Override
    public RiskCategoryResult createCategory(CreateRiskCategoryCommand command) {
        UserContext userContext = UserContextHolder.getRequired();
        ensureManagePermission(userContext);

        String normalizedName = normalizeName(command.name());
        if (riskCategoryRepository.existsByCompanyIdAndNameIgnoreCase(userContext.companyId(), normalizedName)) {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }

        RiskCategory category = new RiskCategory(UUID.randomUUID(), userContext.companyId(), normalizedName);
        RiskCategory saved = saveCategory(category);
        return new RiskCategoryResult(saved.id(), saved.name());
    }

    @Override
    public RiskCategoryResult updateCategory(UUID categoryId, UpdateRiskCategoryCommand command) {
        UserContext userContext = UserContextHolder.getRequired();
        ensureManagePermission(userContext);

        RiskCategory existing = riskCategoryRepository.findByIdAndCompanyId(categoryId, userContext.companyId())
                .orElseThrow(() -> new NotFoundException(CATEGORY_NOT_FOUND_MESSAGE));

        String normalizedName = normalizeName(command.name());
        if (riskCategoryRepository.existsByCompanyIdAndNameIgnoreCaseAndIdNot(
                userContext.companyId(),
                normalizedName,
                existing.id()
        )) {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }

        RiskCategory saved = saveCategory(new RiskCategory(existing.id(), existing.companyId(), normalizedName));
        return new RiskCategoryResult(saved.id(), saved.name());
    }

    @Override
    public void deleteCategory(UUID categoryId) {
        UserContext userContext = UserContextHolder.getRequired();
        ensureManagePermission(userContext);

        RiskCategory existing = riskCategoryRepository.findByIdAndCompanyId(categoryId, userContext.companyId())
                .orElseThrow(() -> new NotFoundException(CATEGORY_NOT_FOUND_MESSAGE));
        try {
            riskCategoryRepository.delete(existing);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Категория используется в рисках и не может быть удалена");
        }
    }

    private void ensureManagePermission(UserContext userContext) {
        boolean hasPermission = permissionCheckPort.hasPermission(
                userContext.userId(),
                userContext.accessToken(),
                Permission.MANAGE_RULES_AND_RISKS
        );
        if (!hasPermission) {
            throw new ForbiddenException("User has no permission: " + Permission.MANAGE_RULES_AND_RISKS.value());
        }
    }

    private String normalizeName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name is required");
        }
        String normalized = name.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        return normalized;
    }

    private RiskCategory saveCategory(RiskCategory category) {
        try {
            return riskCategoryRepository.save(category);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }
    }
}
