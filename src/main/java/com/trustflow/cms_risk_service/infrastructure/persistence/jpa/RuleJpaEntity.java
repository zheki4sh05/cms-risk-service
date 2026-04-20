package com.trustflow.cms_risk_service.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rules")
public class RuleJpaEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String name;

    @Column(name = "condition_expression", nullable = false)
    private String condition;

    @Column(nullable = false)
    private UUID categoryId;

    private UUID riskObjectId;

    @Column(nullable = false)
    private String priority;

    private UUID responsibleUserId;

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<String> actions;

    @Column(nullable = false)
    private boolean enabled;

    private String mechanismScriptName;

    @Column(columnDefinition = "TEXT")
    private String mechanismScriptContent;

    @Column(nullable = false)
    private UUID createdByUserId;

    @Column(nullable = false)
    private Instant savedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public UUID getRiskObjectId() {
        return riskObjectId;
    }

    public void setRiskObjectId(UUID riskObjectId) {
        this.riskObjectId = riskObjectId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public UUID getResponsibleUserId() {
        return responsibleUserId;
    }

    public void setResponsibleUserId(UUID responsibleUserId) {
        this.responsibleUserId = responsibleUserId;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMechanismScriptName() {
        return mechanismScriptName;
    }

    public void setMechanismScriptName(String mechanismScriptName) {
        this.mechanismScriptName = mechanismScriptName;
    }

    public String getMechanismScriptContent() {
        return mechanismScriptContent;
    }

    public void setMechanismScriptContent(String mechanismScriptContent) {
        this.mechanismScriptContent = mechanismScriptContent;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Instant getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(Instant savedAt) {
        this.savedAt = savedAt;
    }
}
