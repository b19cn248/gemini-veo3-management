package com.ptit.google.veo3.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO cho notification message được gửi qua WebSocket
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    
    /**
     * ID của video liên quan đến notification
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
     * Nội dung chi tiết của notification
     */
    private String message;
    
    /**
     * Tên khách hàng (để hiển thị trong notification)
     */
    private String customerName;
    
    /**
     * Người gửi notification (admin/sale/staff)
     */
    private String sender;
    
    /**
     * Người nhận notification
     */
    private String recipient;
    
    /**
     * Thời gian tạo notification
     */
    private LocalDateTime timestamp;
    
    /**
     * Trạng thái mới của video (optional)
     */
    private String newStatus;
    
    /**
     * Trạng thái cũ của video (optional)
     */
    private String oldStatus;
}