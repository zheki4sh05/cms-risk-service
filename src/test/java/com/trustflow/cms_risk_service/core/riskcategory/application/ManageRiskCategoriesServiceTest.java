package com.trustflow.cms_risk_service.core.riskcategory.application;

import com.trustflow.cms_risk_service.core.common.exception.ConflictException;
import com.trustflow.cms_risk_service.core.common.exception.ForbiddenException;
import com.trustflow.cms_risk_service.core.riskcategory.application.port.out.RiskCategoryRepository;
import com.trustflow.cms_risk_service.core.riskcategory.domain.RiskCategory;
import com.trustflow.cms_risk_service.core.rule.application.port.out.PermissionCheckPort;
import com.trustflow.cms_risk_service.core.security.Permission;
import com.trustflow.cms_risk_service.core.security.UserContext;
import com.trustflow.cms_risk_service.core.security.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageRiskCategoriesServiceTest {
    private static final UUID COMPANY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private RiskCategoryRepository riskCategoryRepository;
    @Mock
    private PermissionCheckPort permissionCheckPort;

    @InjectMocks
    private ManageRiskCategoriesService manageRiskCategoriesService;

    @BeforeEach
    void setUp() {
        UserContextHolder.set(new UserContext(USER_ID, COMPANY_ID, "token", USER_ID.toString()));
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void listCategories_returnsCompanyCategoriesWithoutPermissionCheck() {
        UUID categoryId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        when(riskCategoryRepository.findAllByCompanyId(COMPANY_ID))
                .thenReturn(List.of(new RiskCategory(categoryId, COMPANY_ID, "Fraud")));

        ListRiskCategoriesResult result = manageRiskCategoriesService.listCategories();

        assertThat(result.items()).containsExactly(new RiskCategoryResult(categoryId, "Fraud"));
    }

    @Test
    void createCategory_trimsNameBeforeSave() {
        when(permissionCheckPort.hasPermission(eq(USER_ID), eq("token"), eq(Permission.MANAGE_RULES_AND_RISKS)))
                .thenReturn(true);
        when(riskCategoryRepository.existsByCompanyIdAndNameIgnoreCase(COMPANY_ID, "Payments"))
                .thenReturn(false);
        when(riskCategoryRepository.save(org.mockito.ArgumentMatchers.any(RiskCategory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RiskCategoryResult result = manageRiskCategoriesService.createCategory(new CreateRiskCategoryCommand("  Payments  "));

        assertThat(result.name()).isEqualTo("Payments");
        verify(riskCategoryRepository).existsByCompanyIdAndNameIgnoreCase(COMPANY_ID, "Payments");
    }

    @Test
    void createCategory_throwsConflictWhenDuplicateNameExists() {
        when(permissionCheckPort.hasPermission(eq(USER_ID), eq("token"), eq(Permission.MANAGE_RULES_AND_RISKS)))
                .thenReturn(true);
        when(riskCategoryRepository.existsByCompanyIdAndNameIgnoreCase(COMPANY_ID, "Fraud"))
                .thenReturn(true);

        assertThatThrownBy(() -> manageRiskCategoriesService.createCategory(new CreateRiskCategoryCommand("Fraud")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Категория с таким названием уже существует");
    }

    @Test
    void createCategory_throwsForbiddenWhenPermissionMissing() {
        when(permissionCheckPort.hasPermission(eq(USER_ID), eq("token"), eq(Permission.MANAGE_RULES_AND_RISKS)))
                .thenReturn(false);

        assertThatThrownBy(() -> manageRiskCategoriesService.createCategory(new CreateRiskCategoryCommand("Fraud")))
                .isInstanceOf(ForbiddenException.class);
    }
}
