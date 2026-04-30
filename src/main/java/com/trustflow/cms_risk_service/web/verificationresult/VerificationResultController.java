package com.trustflow.cms_risk_service.web.verificationresult;

import com.trustflow.cms_risk_service.core.common.exception.NotFoundException;
import com.trustflow.cms_risk_service.core.security.UserContext;
import com.trustflow.cms_risk_service.core.security.UserContextHolder;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.VerificationResultJpaEntity;
import com.trustflow.cms_risk_service.infrastructure.persistence.jpa.VerificationResultJpaRepository;
import com.trustflow.cms_risk_service.web.verificationresult.dto.ListVerificationResultsResponse;
import com.trustflow.cms_risk_service.web.verificationresult.dto.UpsertVerificationResultRequest;
import com.trustflow.cms_risk_service.web.verificationresult.dto.VerificationResultResponse;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/verification-results")
@RequiredArgsConstructor
public class VerificationResultController {
    private final VerificationResultJpaRepository verificationResultJpaRepository;

    @Operation(summary = "Returns all verification results available for the current company.")
    @GetMapping
    public ListVerificationResultsResponse list() {
        UUID companyId = getCompanyIdFromToken();
        List<VerificationResultResponse> items = verificationResultJpaRepository.findAllByCompanyIdOrderByIdDesc(companyId)
                .stream()
                .map(this::toResponse)
                .toList();
        return new ListVerificationResultsResponse(items);
    }

    @Operation(summary = "Returns a single verification result by ID for the current company.")
    @GetMapping("/{id}")
    public VerificationResultResponse getById(@PathVariable("id") UUID id) {
        VerificationResultJpaEntity entity = findByIdForCurrentCompany(id);
        return toResponse(entity);
    }

    @Operation(summary = "Creates a new verification result for the current company.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VerificationResultResponse create(@Valid @RequestBody UpsertVerificationResultRequest request) {
        UUID companyId = getCompanyIdFromToken();
        VerificationResultJpaEntity entity = new VerificationResultJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setCompanyId(companyId);
        applyUpsertRequest(entity, request);
        VerificationResultJpaEntity saved = verificationResultJpaRepository.save(entity);
        return toResponse(saved);
    }

    @Operation(summary = "Updates an existing verification result by ID for the current company.")
    @PutMapping("/{id}")
    public VerificationResultResponse update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpsertVerificationResultRequest request
    ) {
        VerificationResultJpaEntity entity = findByIdForCurrentCompany(id);
        applyUpsertRequest(entity, request);
        VerificationResultJpaEntity saved = verificationResultJpaRepository.save(entity);
        return toResponse(saved);
    }

    @Operation(summary = "Deletes a verification result by ID for the current company.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") UUID id) {
        VerificationResultJpaEntity entity = findByIdForCurrentCompany(id);
        verificationResultJpaRepository.delete(entity);
    }

    private VerificationResultJpaEntity findByIdForCurrentCompany(UUID id) {
        UUID companyId = getCompanyIdFromToken();
        return verificationResultJpaRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NotFoundException("Verification result not found: " + id));
    }

    private UUID getCompanyIdFromToken() {
        UserContext context = UserContextHolder.getRequired();
        return context.companyId();
    }

    private void applyUpsertRequest(VerificationResultJpaEntity entity, UpsertVerificationResultRequest request) {
        entity.setIntegrationId(request.integrationId());
        entity.setRiskObjectId(request.riskObjectId());
        entity.setDocumentId(request.documentId());
        entity.setData(request.data());
    }

    private VerificationResultResponse toResponse(VerificationResultJpaEntity entity) {
        return new VerificationResultResponse(
                entity.getId(),
                entity.getCompanyId(),
                entity.getIntegrationId(),
                entity.getRiskObjectId(),
                entity.getDocumentId(),
                entity.getData()
        );
    }
}
