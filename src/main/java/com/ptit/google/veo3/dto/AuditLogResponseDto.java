package com.ptit.google.veo3.dto;

import com.ptit.google.veo3.entity.AuditAction;
import com.ptit.google.veo3.entity.EntityType;

import java.time.LocalDateTime;

public class AuditLogResponseDto {
    private Long id;
    private EntityType entityType;
    private Long entityId;
    private AuditAction action;
    private String actionDescription;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String performedBy;
    private LocalDateTime performedAt;
    private String ipAddress;
    private String userAgent;
    private String tenantId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public String getActionDescription() {
        return actionDescription;
    }

    public void setActionDescription(String actionDescription) {
        this.actionDescription = actionDescription;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public LocalDateTime getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(LocalDateTime performedAt) {
        this.performedAt = performedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public boolean isFieldLevelChange() {
        return fieldName != null && !fieldName.trim().isEmpty();
    }

    public boolean isEntityLevelOperation() {
        return action == AuditAction.CREATE || action == AuditAction.DELETE;
    }

    public String getEntityIdentifier() {
        return entityType != null && entityId != null ? 
            entityType.name() + ":" + entityId : "Unknown";
    }

    public boolean hasValueChange() {
        return (oldValue != null && !oldValue.equals(newValue)) || 
               (newValue != null && !newValue.equals(oldValue));
    }
}