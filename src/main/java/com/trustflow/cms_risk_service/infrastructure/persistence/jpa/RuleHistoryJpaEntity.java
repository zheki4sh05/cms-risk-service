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
@Table(name = "rules_history")
public class RuleHistoryJpaEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID companyId;

    private UUID ruleId;

    @Column(nullable = false)
    private String ruleName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    private UUID authorId;

    @Column(name = "condition_expression", columnDefinition = "TEXT")
    private String condition;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "risk_object_id")
    private UUID riskObjectId;

    private String priority;

    @Column(name = "responsible_user_id")
    private UUID responsibleUserId;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> actions;

    private Boolean enabled;

    @Column(name = "mechanism_script_name")
    private String mechanismScriptName;

    @Column(name = "mechanism_script_content", columnDefinition = "TEXT")
    private String mechanismScriptContent;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "saved_at")
    private Instant savedAt;

    @Column(nullable = false)
    private Instant changedAt;

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

    public UUID getRuleId() {
        return ruleId;
    }

    public void setRuleId(UUID ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
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

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }
}
