package com.ptit.google.veo3.entity;

import lombok.Getter;

/**
 * Enum định nghĩa các loại entity có thể được audit trong hệ thống
 * 
 * Tuân thủ Single Responsibility Principle: Chỉ quản lý định nghĩa entity types
 * Tuân thủ Open/Closed Principle: Có thể extend thêm entity types mà không sửa code cũ
 * 
 * Mục đích: Chuẩn hóa entity names trong audit logs để:
 * - Tránh typos khi log audit
 * - Dễ dàng filter và query audit logs
 * - Type-safe khi handle audit data
 * 
 * @author System
 * @since 1.6.0
 */
@Getter
public enum EntityType {
    
    // ============= CORE ENTITIES =============
    /**
     * Video entity - Core business entity
     * Track mọi thay đổi trong lifecycle của video
     */
    VIDEO("Video", "videos", "video"),
    
    /**
     * User entity - Authentication và authorization
     * Track user management operations
     */
    USER("User", "users", "user"),
    
    // ============= FUTURE ENTITIES =============
    /**
     * Staff entity - Nhân viên làm video
     * Dành cho tương lai khi có bảng staff riêng biệt
     */
    STAFF("Staff", "staffs", "staff"),
    
    /**
     * Customer entity - Khách hàng
     * Dành cho tương lai khi tách customer ra entity riêng
     */
    CUSTOMER("Customer", "customers", "customer"),
    
    /**
     * Project entity - Dự án/campaign
     * Dành cho tương lai khi group videos thành projects
     */
    PROJECT("Project", "projects", "project"),
    
    /**
     * Setting entity - System settings
     * Track changes to system configuration
     */
    SETTING("Setting", "settings", "setting");
    
    // ============= ENUM PROPERTIES =============
    /**
     * Entity name - sử dụng trong audit logs (entity_name column)
     * Convention: PascalCase, singular form
     */
    private final String entityName;
    
    /**
     * Table name - tên bảng trong database
     * Convention: snake_case, plural form
     */
    private final String tableName;
    
    /**
     * Entity code - mã ngắn gọn để sử dụng trong URLs, APIs
     * Convention: lowercase, singular form
     */
    private final String entityCode;
    
    /**
     * Constructor
     * 
     * @param entityName Tên entity (PascalCase)
     * @param tableName Tên bảng database (snake_case)
     * @param entityCode Mã entity (lowercase)
     */
    EntityType(String entityName, String tableName, String entityCode) {
        this.entityName = entityName;
        this.tableName = tableName;
        this.entityCode = entityCode;
    }
    
    // ============= UTILITY METHODS =============
    /**
     * Tìm EntityType từ entity name
     * Tuân thủ Fail-Fast Principle: Throw exception nếu không tìm thấy
     * 
     * @param entityName Tên entity cần tìm
     * @return EntityType tương ứng
     * @throws IllegalArgumentException nếu không tìm thấy entity type
     */
    public static EntityType fromEntityName(String entityName) {
        if (entityName == null || entityName.trim().isEmpty()) {
            throw new IllegalArgumentException("Entity name cannot be null or empty");
        }
        
        for (EntityType type : values()) {
            if (type.entityName.equalsIgnoreCase(entityName.trim())) {
                return type;
            }
        }
        
        throw new IllegalArgumentException("Unknown entity type: " + entityName);
    }
    
    /**
     * Tìm EntityType từ table name
     * 
     * @param tableName Tên bảng cần tìm
     * @return EntityType tương ứng
     * @throws IllegalArgumentException nếu không tìm thấy entity type
     */
    public static EntityType fromTableName(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }
        
        for (EntityType type : values()) {
            if (type.tableName.equalsIgnoreCase(tableName.trim())) {
                return type;
            }
        }
        
        throw new IllegalArgumentException("Unknown table name: " + tableName);
    }
    
    /**
     * Tìm EntityType từ entity code
     * 
     * @param entityCode Mã entity cần tìm
     * @return EntityType tương ứng
     * @throws IllegalArgumentException nếu không tìm thấy entity type
     */
    public static EntityType fromEntityCode(String entityCode) {
        if (entityCode == null || entityCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Entity code cannot be null or empty");
        }
        
        for (EntityType type : values()) {
            if (type.entityCode.equalsIgnoreCase(entityCode.trim())) {
                return type;
            }
        }
        
        throw new IllegalArgumentException("Unknown entity code: " + entityCode);
    }
    
    /**
     * Kiểm tra có phải là core entity không
     * Core entities là những entities chính của hệ thống hiện tại
     * 
     * @return true nếu là core entity
     */
    public boolean isCoreEntity() {
        return this == VIDEO || this == USER;
    }
    
    /**
     * Kiểm tra có phải là future entity không
     * Future entities là những entities dự kiến sẽ có trong tương lai
     * 
     * @return true nếu là future entity
     */
    public boolean isFutureEntity() {
        return this == STAFF || this == CUSTOMER || this == PROJECT || this == SETTING;
    }
    
    /**
     * Get display name cho UI
     * Có thể customize theo ngôn ngữ trong tương lai
     * 
     * @return Tên hiển thị
     */
    public String getDisplayName() {
        return switch (this) {
            case VIDEO -> "Video";
            case USER -> "Người dùng";
            case STAFF -> "Nhân viên";
            case CUSTOMER -> "Khách hàng";
            case PROJECT -> "Dự án";
            case SETTING -> "Cài đặt";
        };
    }
    
    /**
     * Get icon class cho UI (dành cho frontend)
     * 
     * @return CSS class name cho icon
     */
    public String getIconClass() {
        return switch (this) {
            case VIDEO -> "fa-video";
            case USER -> "fa-user";
            case STAFF -> "fa-users";
            case CUSTOMER -> "fa-user-tie";
            case PROJECT -> "fa-folder-open";
            case SETTING -> "fa-cogs";
        };
    }
}