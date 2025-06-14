package com.ptit.google.veo3.entity;

import lombok.Getter;

/**
 * Enum định nghĩa các loại hành động có thể được audit trong hệ thống
 * 
 * Tuân thủ Single Responsibility Principle: Chỉ quản lý định nghĩa các action types
 * Tuân thủ Open/Closed Principle: Có thể extend thêm actions mới mà không sửa code cũ
 * 
 * @author System
 * @since 1.6.0
 */
@Getter
public enum AuditAction {
    
    // ============= CRUD OPERATIONS =============
    /**
     * Tạo mới entity (CREATE)
     * Được trigger khi tạo mới video, user, hoặc entity khác
     */
    CREATE("CREATE", "Tạo mới"),
    
    /**
     * Cập nhật entity (UPDATE) - generic update
     * Được trigger khi có bất kỳ field nào thay đổi
     */
    UPDATE("UPDATE", "Cập nhật"),
    
    /**
     * Xóa entity (DELETE)
     * Bao gồm cả soft delete và hard delete
     */
    DELETE("DELETE", "Xóa"),
    
    // ============= VIDEO BUSINESS OPERATIONS =============
    /**
     * Thay đổi trạng thái video
     * Ví dụ: CHUA_AI_NHAN -> DANG_LAM -> DA_XONG
     */
    UPDATE_STATUS("UPDATE_STATUS", "Thay đổi trạng thái"),
    
    /**
     * Thay đổi trạng thái giao hàng
     * Ví dụ: CHUA_GUI -> DA_GUI
     */
    UPDATE_DELIVERY_STATUS("UPDATE_DELIVERY_STATUS", "Thay đổi trạng thái giao hàng"),
    
    /**
     * Thay đổi trạng thái thanh toán
     * Ví dụ: CHUA_THANH_TOAN -> DA_THANH_TOAN
     */
    UPDATE_PAYMENT_STATUS("UPDATE_PAYMENT_STATUS", "Thay đổi trạng thái thanh toán"),
    
    /**
     * Gán nhân viên cho video
     * Khi admin assign staff để làm video
     */
    ASSIGN_STAFF("ASSIGN_STAFF", "Gán nhân viên"),
    
    /**
     * Hủy gán nhân viên
     * Khi unassign staff khỏi video
     */
    UNASSIGN_STAFF("UNASSIGN_STAFF", "Hủy gán nhân viên"),
    
    /**
     * Cập nhật link video hoàn thành
     * Khi staff upload video finished
     */
    UPDATE_VIDEO_URL("UPDATE_VIDEO_URL", "Cập nhật link video"),
    
    /**
     * Cập nhật giá trị đơn hàng
     * Khi thay đổi order value hoặc pricing
     */
    UPDATE_ORDER_VALUE("UPDATE_ORDER_VALUE", "Cập nhật giá trị đơn hàng"),
    
    /**
     * Cập nhật giá bán
     * Khi recalculate pricing based on duration/value
     */
    UPDATE_PRICE("UPDATE_PRICE", "Cập nhật giá bán"),
    
    // ============= SYSTEM OPERATIONS =============
    /**
     * Tự động reset video
     * Khi system auto-reset video sau 15 phút không hoạt động
     */
    AUTO_RESET("AUTO_RESET", "Tự động reset"),
    
    /**
     * Khách hàng phê duyệt
     * Khi customer approve video
     */
    CUSTOMER_APPROVAL("CUSTOMER_APPROVAL", "Khách hàng phê duyệt"),
    
    /**
     * Khách hàng yêu cầu sửa
     * Khi customer request changes
     */
    CUSTOMER_REVISION("CUSTOMER_REVISION", "Khách hàng yêu cầu sửa"),
    
    /**
     * Hủy video
     * Khi cancel video order
     */
    CANCEL("CANCEL", "Hủy video"),
    
    // ============= ADMIN OPERATIONS =============
    /**
     * Thao tác của admin
     * Các actions đặc biệt chỉ admin mới có thể thực hiện
     */
    ADMIN_OVERRIDE("ADMIN_OVERRIDE", "Admin can thiệp"),
    
    /**
     * Khôi phục từ trash
     * Khi restore deleted items
     */
    RESTORE("RESTORE", "Khôi phục"),
    
    /**
     * Export dữ liệu
     * Khi admin export audit logs hoặc reports
     */
    EXPORT("EXPORT", "Xuất dữ liệu");
    
    // ============= ENUM PROPERTIES =============
    /**
     * Mã action - sử dụng cho database storage
     */
    private final String code;
    
    /**
     * Mô tả action - sử dụng cho UI display
     */
    private final String description;
    
    /**
     * Constructor
     * 
     * @param code Mã action
     * @param description Mô tả action
     */
    AuditAction(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    // ============= UTILITY METHODS =============
    /**
     * Tìm AuditAction từ code
     * Tuân thủ Fail-Fast Principle: Throw exception nếu không tìm thấy
     * 
     * @param code Mã action cần tìm
     * @return AuditAction tương ứng
     * @throws IllegalArgumentException nếu không tìm thấy action
     */
    public static AuditAction fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Action code cannot be null or empty");
        }
        
        for (AuditAction action : values()) {
            if (action.code.equalsIgnoreCase(code.trim())) {
                return action;
            }
        }
        
        throw new IllegalArgumentException("Unknown audit action code: " + code);
    }
    
    /**
     * Kiểm tra có phải là business operation không
     * Dùng để phân loại actions cho reporting và filtering
     * 
     * @return true nếu là business operation
     */
    public boolean isBusinessOperation() {
        return this == UPDATE_STATUS || 
               this == UPDATE_DELIVERY_STATUS || 
               this == UPDATE_PAYMENT_STATUS ||
               this == ASSIGN_STAFF || 
               this == UNASSIGN_STAFF || 
               this == UPDATE_VIDEO_URL ||
               this == CUSTOMER_APPROVAL || 
               this == CUSTOMER_REVISION;
    }
    
    /**
     * Kiểm tra có phải là system operation không
     * 
     * @return true nếu là system operation
     */
    public boolean isSystemOperation() {
        return this == AUTO_RESET || 
               this == CREATE || 
               this == UPDATE || 
               this == DELETE;
    }
    
    /**
     * Kiểm tra có phải là admin operation không
     * 
     * @return true nếu là admin operation
     */
    public boolean isAdminOperation() {
        return this == ADMIN_OVERRIDE || 
               this == RESTORE || 
               this == EXPORT || 
               this == CANCEL;
    }
}