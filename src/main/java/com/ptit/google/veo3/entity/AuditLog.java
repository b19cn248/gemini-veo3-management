package com.ptit.google.veo3.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity đại diện cho bảng audit_logs trong database
 * 
 * Chức năng: Track toàn bộ lịch sử thay đổi của các entities trong hệ thống
 * 
 * Tuân thủ SOLID Principles:
 * - Single Responsibility: Chỉ quản lý audit log data
 * - Open/Closed: Có thể extend thêm fields mà không sửa code cũ
 * - Liskov Substitution: Implement Serializable consistently
 * - Interface Segregation: Không implement unnecessary interfaces
 * - Dependency Inversion: Depend on abstractions (enums) not concrete values
 * 
 * Design Patterns:
 * - Value Object: Immutable audit records
 * - Builder Pattern: Sử dụng Lombok @Builder
 * 
 * Performance Considerations:
 * - Không extend BaseEntity để tránh overhead
 * - Chỉ có minimal required fields
 * - Indexing strategy được define trong Liquibase
 * 
 * Security Considerations:
 * - Audit logs không bao giờ được update sau khi tạo (immutable)
 * - Không có soft delete - audit logs phải persistent forever
 * - Sensitive data được mask trong old_value/new_value
 * 
 * @author System
 * @since 1.6.0
 */
@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id") // Chỉ sử dụng id cho equals/hashCode để tối ưu performance
public class AuditLog implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // ============= PRIMARY KEY =============
    /**
     * Primary key - ID tự tăng
     * Sử dụng IDENTITY strategy cho MySQL compatibility
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    // ============= ENTITY INFORMATION =============
    /**
     * Tên entity bị thay đổi (Video, User, Staff, etc.)
     * Sử dụng enum để đảm bảo consistency và type safety
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_name", nullable = false, length = 50)
    private EntityType entityType;
    
    /**
     * ID của entity bị thay đổi
     * Không sử dụng foreign key để đảm bảo audit logs persistent khi entity bị xóa
     */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;
    
    // ============= ACTION INFORMATION =============
    /**
     * Loại hành động được thực hiện (CREATE, UPDATE, DELETE, etc.)
     * Sử dụng enum để standardize action types
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private AuditAction action;
    
    /**
     * Mô tả chi tiết về hành động
     * Ví dụ: "Thay đổi trạng thái video từ 'Chưa ai nhận' thành 'Đang làm'"
     */
    @Column(name = "action_description", length = 500)
    private String actionDescription;
    
    // ============= FIELD-LEVEL CHANGE TRACKING =============
    /**
     * Tên field bị thay đổi (nullable cho CREATE/DELETE operations)
     * Ví dụ: "status", "assignedStaff", "videoUrl"
     */
    @Column(name = "field_name", length = 100)
    private String fieldName;
    
    /**
     * Giá trị cũ của field (JSON format cho complex objects)
     * Null đối với CREATE operations
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;
    
    /**
     * Giá trị mới của field (JSON format cho complex objects)
     * Null đối với DELETE operations
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;
    
    // ============= USER & SESSION INFORMATION =============
    /**
     * Username của người thực hiện hành động
     * Lấy từ JWT token hoặc system context
     */
    @Column(name = "performed_by", nullable = false, length = 255)
    private String performedBy;
    
    /**
     * Thời điểm thực hiện hành động
     * Sử dụng @CreationTimestamp để auto-populate
     * Không cho phép update sau khi tạo
     */
    @CreationTimestamp
    @Column(name = "performed_at", nullable = false, updatable = false)
    private LocalDateTime performedAt;
    
    // ============= SECURITY & TRACKING INFORMATION =============
    /**
     * IP address của client thực hiện hành động
     * Dùng cho security auditing và forensics
     */
    @Column(name = "ip_address", length = 45) // Đủ cho IPv6
    private String ipAddress;
    
    /**
     * User agent string của browser/client
     * Dùng để identify browser, OS, device
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    // ============= MULTI-TENANT SUPPORT =============
    /**
     * Tenant ID cho multi-tenant architecture
     * Null cho single-tenant deployment
     */
    @Column(name = "tenant_id", length = 100)
    private String tenantId;
    
    // ============= BUSINESS METHODS =============
    /**
     * Kiểm tra có phải field-level change không
     * 
     * @return true nếu là field-level change
     */
    public boolean isFieldLevelChange() {
        return fieldName != null && !fieldName.trim().isEmpty();
    }
    
    /**
     * Kiểm tra có phải entity-level operation không (CREATE, DELETE)
     * 
     * @return true nếu là entity-level operation
     */
    public boolean isEntityLevelOperation() {
        return action == AuditAction.CREATE || action == AuditAction.DELETE;
    }
    
    /**
     * Get entity identifier cho display
     * Format: EntityType:ID (ví dụ: Video:123)
     * 
     * @return String identifier
     */
    public String getEntityIdentifier() {
        return entityType.getEntityName() + ":" + entityId;
    }
    
    /**
     * Kiểm tra có change value không
     * 
     * @return true nếu có thay đổi giá trị
     */
    public boolean hasValueChange() {
        return (oldValue != null && !oldValue.equals(newValue)) ||
               (newValue != null && !newValue.equals(oldValue));
    }
    
    /**
     * Get formatted description cho display
     * Kết hợp action description với field change info
     * 
     * @return Formatted description
     */
    public String getFormattedDescription() {
        StringBuilder sb = new StringBuilder();
        
        if (actionDescription != null && !actionDescription.trim().isEmpty()) {
            sb.append(actionDescription);
        } else {
            // Fallback description từ action và entity
            sb.append(action.getDescription())
              .append(" ")
              .append(entityType.getDisplayName());
            
            if (entityId != null) {
                sb.append(" #").append(entityId);
            }
        }
        
        // Thêm field change info nếu có
        if (isFieldLevelChange() && hasValueChange()) {
            sb.append(" (")
              .append(fieldName)
              .append(": ");
            
            if (oldValue != null) {
                sb.append("'").append(oldValue).append("'");
            } else {
                sb.append("null");
            }
            
            sb.append(" → ");
            
            if (newValue != null) {
                sb.append("'").append(newValue).append("'");
            } else {
                sb.append("null");
            }
            
            sb.append(")");
        }
        
        return sb.toString();
    }
    
    // ============= LIFECYCLE METHODS =============
    /**
     * Pre-persist validation
     * Đảm bảo data integrity trước khi lưu database
     */
    @PrePersist
    public void prePersist() {
        // Validate required fields
        if (entityType == null) {
            throw new IllegalStateException("EntityType cannot be null");
        }
        
        if (entityId == null) {
            throw new IllegalStateException("EntityId cannot be null");
        }
        
        if (action == null) {
            throw new IllegalStateException("Action cannot be null");
        }
        
        if (performedBy == null || performedBy.trim().isEmpty()) {
            throw new IllegalStateException("PerformedBy cannot be null or empty");
        }
        
        // Set default values
        if (performedAt == null) {
            performedAt = LocalDateTime.now();
        }
        
        // Trim string values
        if (actionDescription != null) {
            actionDescription = actionDescription.trim();
        }
        
        if (fieldName != null) {
            fieldName = fieldName.trim();
        }
        
        if (performedBy != null) {
            performedBy = performedBy.trim();
        }
    }
}