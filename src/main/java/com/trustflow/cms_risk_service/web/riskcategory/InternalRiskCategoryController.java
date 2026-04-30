package com.trustflow.cms_risk_service.web.riskcategory;

import com.trustflow.cms_risk_service.core.common.exception.ForbiddenException;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.RiskCategoryJpaRepository;
import com.trustflow.cms_risk_service.web.riskcategory.dto.ListRiskCategoriesResponse;
import com.trustflow.cms_risk_service.web.riskcategory.dto.RiskCategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/risk-categories")
@RequiredArgsConstructor
public class InternalRiskCategoryController {
    private static final String COMPANY_ID_HEADER = "CompanyId";
    private static final String COMPANY_ID_LEGACY_HEADER = "companyId";

    private final RiskCategoryJpaRepository riskCategoryJpaRepository;

    @Operation(summary = "Returns risk categories for internal localhost-only integrations.")
    @GetMapping
    public ListRiskCategoriesResponse listCategories(HttpServletRequest request) {
        validateRequestIsFromLocalhost(request);
        UUID companyId = extractCompanyId(request);
        return new ListRiskCategoriesResponse(
                riskCategoryJpaRepository.findAllByCompanyIdOrderByNameAsc(companyId)
                        .stream()
                        .map(entity -> new RiskCategoryResponse(entity.getId(), entity.getName()))
                        .toList()
        );
    }

    private void validateRequestIsFromLocalhost(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        boolean localhost = "127.0.0.1".equals(remoteAddress)
                || "0:0:0:0:0:0:0:1".equals(remoteAddress)
                || "::1".equals(remoteAddress)
                || "localhost".equalsIgnoreCase(remoteAddress);
        if (!localhost) {
            throw new ForbiddenException("Internal endpoint is available only from localhost");
        }
    }

    private UUID extractCompanyId(HttpServletRequest request) {
        String companyIdValue = request.getHeader(COMPANY_ID_HEADER);
        if (companyIdValue == null || companyIdValue.isBlank()) {
            companyIdValue = request.getHeader(COMPANY_ID_LEGACY_HEADER);
        }
        if (companyIdValue == null || companyIdValue.isBlank()) {
            throw new IllegalArgumentException("CompanyId header is required");
        }
        try {
            return UUID.fromString(companyIdValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("CompanyId header must be valid UUID");
        }
    }
}
