package com.ptit.google.veo3.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptit.google.veo3.entity.*;
import com.ptit.google.veo3.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service class chứa business logic cho audit logging
 * 
 * Chức năng:
 * - Track toàn bộ CRUD operations trên entities
 * - Field-level change detection và logging
 * - Async audit logging để không ảnh hưởng performance
 * - Context capture (user, IP, user agent) từ request
 * 
 * Tuân thủ SOLID Principles:
 * - Single Responsibility: Chỉ quản lý audit logging
 * - Open/Closed: Có thể extend cho new entity types
 * - Liskov Substitution: Consistent behavior cho all entity types
 * - Interface Segregation: Clear method interfaces
 * - Dependency Inversion: Depend on abstractions (repository, service interfaces)
 * 
 * Performance Considerations:
 * - Async processing để không block main operations
 * - Efficient field comparison using reflection
 * - JSON serialization cho complex objects
 * - Batch processing cho multiple changes
 * 
 * Security Considerations:
 * - Sensitive field masking
 * - User context validation
 * - SQL injection prevention through parameterized queries
 * 
 * @author System
 * @since 1.6.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;
    
    // ============= CORE AUDIT METHODS =============
    
    /**
     * Log entity creation
     * Async operation để không ảnh hưởng performance
     * 
     * @param entity Entity vừa được tạo
     * @param <T> Entity type
     * @return CompletableFuture for async processing
     */
    @Async
    @Transactional
    public <T extends BaseEntity> CompletableFuture<AuditLog> logEntityCreation(T entity) {
        try {
            EntityType entityType = determineEntityType(entity);
            String username = getCurrentUsername();
            
            AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId(entity.getId())
                .action(AuditAction.CREATE)
                .actionDescription(buildCreateDescription(entityType, entity))
                .newValue(serializeEntityToJson(entity))
                .performedBy(username)
                .ipAddress(getCurrentIpAddress())
                .userAgent(getCurrentUserAgent())
                .tenantId(getCurrentTenantId())
                .build();
            
            AuditLog savedAudit = auditLogRepository.save(auditLog);
            
            log.info("Audit logged: Entity {} created by {} - ID: {}", 
                entityType.getEntityName(), username, entity.getId());
            
            return CompletableFuture.completedFuture(savedAudit);
            
        } catch (Exception e) {
            log.error("Failed to log entity creation for {}: {}", 
                entity.getClass().getSimpleName(), e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Log entity update với field-level change detection
     * 
     * @param oldEntity Entity state trước khi update
     * @param newEntity Entity state sau khi update  
     * @param <T> Entity type
     * @return CompletableFuture for async processing
     */
    @Async
    @Transactional
    public <T extends BaseEntity> CompletableFuture<List<AuditLog>> logEntityUpdate(T oldEntity, T newEntity) {
        try {
            EntityType entityType = determineEntityType(newEntity);
            String username = getCurrentUsername();
            List<AuditLog> auditLogs = new java.util.ArrayList<>();
            
            // Detect field-level changes
            Map<String, FieldChange> changes = detectFieldChanges(oldEntity, newEntity);
            
            if (changes.isEmpty()) {
                log.debug("No changes detected for {} ID: {}", entityType.getEntityName(), newEntity.getId());
                return CompletableFuture.completedFuture(auditLogs);
            }
            
            // Tạo audit log cho mỗi field thay đổi
            for (Map.Entry<String, FieldChange> entry : changes.entrySet()) {
                String fieldName = entry.getKey();
                FieldChange change = entry.getValue();
                
                AuditAction action = determineActionForField(fieldName);
                
                AuditLog auditLog = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(newEntity.getId())
                    .action(action)
                    .actionDescription(buildUpdateDescription(entityType, fieldName, change))
                    .fieldName(fieldName)
                    .oldValue(serializeValue(change.getOldValue()))
                    .newValue(serializeValue(change.getNewValue()))
                    .performedBy(username)
                    .ipAddress(getCurrentIpAddress())
                    .userAgent(getCurrentUserAgent())
                    .tenantId(getCurrentTenantId())
                    .build();
                
                auditLogs.add(auditLog);
            }
            
            // Batch save để tối ưu performance
            List<AuditLog> savedAudits = auditLogRepository.saveAll(auditLogs);
            
            log.info("Audit logged: {} fields updated for {} ID: {} by {}", 
                changes.size(), entityType.getEntityName(), newEntity.getId(), username);
            
            return CompletableFuture.completedFuture(savedAudits);
            
        } catch (Exception e) {
            log.error("Failed to log entity update for {}: {}", 
                newEntity.getClass().getSimpleName(), e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Log entity deletion
     * 
     * @param entity Entity sắp bị xóa
     * @param <T> Entity type
     * @return CompletableFuture for async processing
     */
    @Async
    @Transactional  
    public <T extends BaseEntity> CompletableFuture<AuditLog> logEntityDeletion(T entity) {
        try {
            EntityType entityType = determineEntityType(entity);
            String username = getCurrentUsername();
            
            AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId(entity.getId())
                .action(AuditAction.DELETE)
                .actionDescription(buildDeleteDescription(entityType, entity))
                .oldValue(serializeEntityToJson(entity))
                .performedBy(username)
                .ipAddress(getCurrentIpAddress())
                .userAgent(getCurrentUserAgent())
                .tenantId(getCurrentTenantId())
                .build();
            
            AuditLog savedAudit = auditLogRepository.save(auditLog);
            
            log.info("Audit logged: Entity {} deleted by {} - ID: {}", 
                entityType.getEntityName(), username, entity.getId());
            
            return CompletableFuture.completedFuture(savedAudit);
            
        } catch (Exception e) {
            log.error("Failed to log entity deletion for {}: {}", 
                entity.getClass().getSimpleName(), e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    // ============= VIDEO SPECIFIC METHODS =============
    
    /**
     * Log video business operations (assign staff, status change, etc.)
     * 
     * @param videoId Video ID
     * @param action Business action
     * @param description Action description
     * @param fieldName Field được thay đổi (nullable)
     * @param oldValue Giá trị cũ (nullable)
     * @param newValue Giá trị mới (nullable)
     * @return CompletableFuture for async processing
     */
    @Async
    @Transactional
    public CompletableFuture<AuditLog> logVideoBusinessAction(
            Long videoId,
            AuditAction action, 
            String description,
            String fieldName,
            Object oldValue,
            Object newValue) {
        
        try {
            String username = getCurrentUsername();
            
            AuditLog auditLog = AuditLog.builder()
                .entityType(EntityType.VIDEO)
                .entityId(videoId)
                .action(action)
                .actionDescription(description)
                .fieldName(fieldName)
                .oldValue(serializeValue(oldValue))
                .newValue(serializeValue(newValue))
                .performedBy(username)
                .ipAddress(getCurrentIpAddress())
                .userAgent(getCurrentUserAgent())
                .tenantId(getCurrentTenantId())
                .build();
            
            AuditLog savedAudit = auditLogRepository.save(auditLog);
            
            log.info("Video business action logged: {} for Video ID: {} by {}", 
                action.getDescription(), videoId, username);
            
            return CompletableFuture.completedFuture(savedAudit);
            
        } catch (Exception e) {
            log.error("Failed to log video business action {} for Video ID {}: {}", 
                action, videoId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    // ============= QUERY METHODS =============
    
    /**
     * Lấy lịch sử của một video
     * 
     * @param videoId Video ID
     * @return List audit logs
     */
    public List<AuditLog> getVideoHistory(Long videoId) {
        try {
            return auditLogRepository.findVideoHistory(videoId);
        } catch (Exception e) {
            log.error("Failed to retrieve video history for ID {}: {}", videoId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve video history", e);
        }
    }
    
    /**
     * Lấy lịch sử video với pagination
     * 
     * @param videoId Video ID
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    public Page<AuditLog> getVideoHistoryPaged(Long videoId, Pageable pageable) {
        try {
            return auditLogRepository.findVideoHistoryPaged(videoId, pageable);
        } catch (Exception e) {
            log.error("Failed to retrieve paged video history for ID {}: {}", videoId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve video history", e);
        }
    }
    
    /**
     * Search audit logs với filters
     * 
     * @param entityType Entity type filter (nullable)
     * @param action Action filter (nullable)
     * @param performedBy User filter (nullable)
     * @param fromDate From date filter (nullable)
     * @param toDate To date filter (nullable)
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    public Page<AuditLog> searchAuditLogs(
            EntityType entityType,
            AuditAction action,
            String performedBy,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {
        
        try {
            return auditLogRepository.findAuditLogsByFilters(
                entityType, action, performedBy, fromDate, toDate, pageable);
        } catch (Exception e) {
            log.error("Failed to search audit logs: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search audit logs", e);
        }
    }
    
    // ============= UTILITY METHODS =============
    
    /**
     * Determine entity type từ entity class
     */
    private <T extends BaseEntity> EntityType determineEntityType(T entity) {
        String className = entity.getClass().getSimpleName();
        return switch (className) {
            case "Video" -> EntityType.VIDEO;
            case "User" -> EntityType.USER;
            default -> throw new IllegalArgumentException("Unsupported entity type: " + className);
        };
    }
    
    /**
     * Detect field changes between old and new entity
     */
    private <T extends BaseEntity> Map<String, FieldChange> detectFieldChanges(T oldEntity, T newEntity) {
        Map<String, FieldChange> changes = new HashMap<>();
        
        try {
            Field[] fields = oldEntity.getClass().getDeclaredFields();
            
            for (Field field : fields) {
                // Skip audit fields từ BaseEntity
                if (isAuditField(field.getName())) {
                    continue;
                }
                
                field.setAccessible(true);
                Object oldValue = field.get(oldEntity);
                Object newValue = field.get(newEntity);
                
                if (!java.util.Objects.equals(oldValue, newValue)) {
                    changes.put(field.getName(), new FieldChange(oldValue, newValue));
                }
            }
            
        } catch (IllegalAccessException e) {
            log.error("Failed to detect field changes: {}", e.getMessage(), e);
        }
        
        return changes;
    }
    
    /**
     * Check if field is audit field (từ BaseEntity)
     */
    private boolean isAuditField(String fieldName) {
        return "id".equals(fieldName) ||
               "createdAt".equals(fieldName) ||
               "updatedAt".equals(fieldName) ||
               "createdBy".equals(fieldName) ||
               "updatedBy".equals(fieldName) ||
               "isDeleted".equals(fieldName);
    }
    
    /**
     * Determine audit action dựa trên field name
     */
    private AuditAction determineActionForField(String fieldName) {
        return switch (fieldName) {
            case "status" -> AuditAction.UPDATE_STATUS;
            case "deliveryStatus" -> AuditAction.UPDATE_DELIVERY_STATUS;
            case "paymentStatus" -> AuditAction.UPDATE_PAYMENT_STATUS;
            case "assignedStaff", "assignedUser" -> AuditAction.ASSIGN_STAFF;
            case "videoUrl" -> AuditAction.UPDATE_VIDEO_URL;
            case "orderValue" -> AuditAction.UPDATE_ORDER_VALUE;
            case "price" -> AuditAction.UPDATE_PRICE;
            case "customerApproved" -> AuditAction.CUSTOMER_APPROVAL;
            default -> AuditAction.UPDATE;
        };
    }
    
    /**
     * Serialize entity thành JSON string
     */
    private String serializeEntityToJson(Object entity) {
        try {
            // Mask sensitive fields trước khi serialize
            Map<String, Object> maskedData = maskSensitiveFields(entity);
            return objectMapper.writeValueAsString(maskedData);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize entity to JSON: {}", e.getMessage());
            return entity.toString();
        }
    }
    
    /**
     * Serialize value thành string
     */
    private String serializeValue(Object value) {
        if (value == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
    
    /**
     * Mask sensitive fields trong entity data
     */
    private Map<String, Object> maskSensitiveFields(Object entity) {
        // Implementation để mask sensitive data
        // Ví dụ: mask password, credit card, etc.
        Map<String, Object> result = new HashMap<>();
        
        try {
            Field[] fields = entity.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(entity);
                
                // Mask sensitive fields
                if (isSensitiveField(field.getName())) {
                    result.put(field.getName(), "***MASKED***");
                } else {
                    result.put(field.getName(), value);
                }
            }
        } catch (IllegalAccessException e) {
            log.warn("Failed to mask sensitive fields: {}", e.getMessage());
            result.put("data", entity.toString());
        }
        
        return result;
    }
    
    /**
     * Check if field contains sensitive data
     */
    private boolean isSensitiveField(String fieldName) {
        String lowerFieldName = fieldName.toLowerCase();
        return lowerFieldName.contains("password") ||
               lowerFieldName.contains("token") ||
               lowerFieldName.contains("secret") ||
               lowerFieldName.contains("key");
    }
    
    // ============= CONTEXT CAPTURE METHODS =============
    
    /**
     * Get current username từ JWT token
     */
    private String getCurrentUsername() {
        try {
            return jwtTokenService.getCurrentUserNameFromJwt();
        } catch (Exception e) {
            log.warn("Failed to get current username: {}", e.getMessage());
            return "system";
        }
    }
    
    /**
     * Get current IP address từ request
     */
    private String getCurrentIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Failed to get IP address: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Get current user agent từ request
     */
    private String getCurrentUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest().getHeader("User-Agent");
            }
        } catch (Exception e) {
            log.debug("Failed to get user agent: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Get current tenant ID từ request header
     */
    private String getCurrentTenantId() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest().getHeader("db");
            }
        } catch (Exception e) {
            log.debug("Failed to get tenant ID: {}", e.getMessage());
        }
        return null;
    }
    
    // ============= DESCRIPTION BUILDERS =============
    
    private String buildCreateDescription(EntityType entityType, BaseEntity entity) {
        return switch (entityType) {
            case VIDEO -> {
                Video video = (Video) entity;
                yield String.format("Tạo mới video cho khách hàng: %s", video.getCustomerName());
            }
            case USER -> {
                User user = (User) entity;
                yield String.format("Tạo mới người dùng: %s", user.getUsername());
            }
            default -> String.format("Tạo mới %s", entityType.getDisplayName());
        };
    }
    
    private String buildUpdateDescription(EntityType entityType, String fieldName, FieldChange change) {
        String entityName = entityType.getDisplayName();
        String oldVal = change.getOldValue() != null ? String.valueOf(change.getOldValue()) : "null";
        String newVal = change.getNewValue() != null ? String.valueOf(change.getNewValue()) : "null";
        
        return switch (fieldName) {
            case "status" -> String.format("Thay đổi trạng thái %s từ '%s' thành '%s'", entityName, oldVal, newVal);
            case "assignedStaff" -> String.format("Gán %s cho nhân viên: %s", entityName, newVal);
            case "videoUrl" -> String.format("Cập nhật link video: %s", newVal);
            case "price" -> String.format("Cập nhật giá %s từ %s thành %s", entityName, oldVal, newVal);
            default -> String.format("Cập nhật %s.%s từ '%s' thành '%s'", entityName, fieldName, oldVal, newVal);
        };
    }
    
    private String buildDeleteDescription(EntityType entityType, BaseEntity entity) {
        return switch (entityType) {
            case VIDEO -> {
                Video video = (Video) entity;
                yield String.format("Xóa video của khách hàng: %s", video.getCustomerName());
            }
            case USER -> {
                User user = (User) entity;
                yield String.format("Xóa người dùng: %s", user.getUsername());
            }
            default -> String.format("Xóa %s", entityType.getDisplayName());
        };
    }
    
    // ============= INNER CLASSES =============
    
    /**
     * Inner class để represent field change
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class FieldChange {
        private Object oldValue;
        private Object newValue;
    }
}