package com.ptit.google.veo3.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO cho notification response trong REST API
 * 
 * Được sử dụng khi:
 * - Trả về danh sách notifications cho frontend
 * - Response cho các API notification CRUD
 * - Hiển thị notification details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {
    
    /**
     * ID của notification
     */
    private Long id;
    
    /**
     * ID của video liên quan (có thể null)
     */
    private Long videoId;
    
    /**
     * Loại notification
     */
    private NotificationType type;
    
    /**
     * Tiêu đề notification
     */
    private String title;
    
    /**
     * Nội dung chi tiết
     */
    private String message;
    
    /**
     * Tên khách hàng
     */
    private String customerName;
    
    /**
     * Người gửi notification
     */
    private String sender;
    
    /**
     * Người nhận notification
     */
    private String recipient;
    
    /**
     * Trạng thái mới (nếu có)
     */
    private String newStatus;
    
    /**
     * Trạng thái cũ (nếu có)
     */
    private String oldStatus;
    
    /**
     * Đã đọc hay chưa
     */
    private Boolean isRead;
    
    /**
     * Thời gian tạo notification
     */
    private LocalDateTime createdAt;
    
    /**
     * Thời gian đọc notification (null nếu chưa đọc)
     */
    private LocalDateTime readAt;
    
    /**
     * Tenant ID
     */
    private String tenantId;
    
    /**
     * Helper property để check unread
     */
    public boolean isUnread() {
        return !Boolean.TRUE.equals(isRead);
    }
    
    /**
     * Helper property để format display text
     */
    public String getTypeDisplayName() {
        return type != null ? type.getDisplayName() : null;
    }
}