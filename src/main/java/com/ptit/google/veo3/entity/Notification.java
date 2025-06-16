package com.ptit.google.veo3.entity;

import com.ptit.google.veo3.dto.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity represent một notification trong hệ thống
 * 
 * Entity này lưu trữ tất cả notifications được gửi đến users,
 * cho phép:
 * - Hiển thị lịch sử notifications
 * - Đánh dấu đã đọc/chưa đọc
 * - Query notifications theo user và thời gian
 * - Multi-tenant support
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_recipient_created", columnList = "recipientUsername, createdAt"),
    @Index(name = "idx_recipient_unread", columnList = "recipientUsername, isRead"),
    @Index(name = "idx_video_id", columnList = "videoId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {


    /**
     * Video liên quan đến notification (có thể null cho system notifications)
     */
    @Column(name = "video_id")
    private Long videoId;

    /**
     * Username của người nhận notification
     * Phải khớp với username từ JWT token
     */
    @Column(name = "recipient_username", nullable = false, length = 255)
    private String recipientUsername;

    /**
     * Username của người gửi notification
     */
    @Column(name = "sender_username", nullable = false, length = 255)
    private String senderUsername;

    /**
     * Loại notification
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    /**
     * Tiêu đề notification
     */
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    /**
     * Nội dung chi tiết notification
     */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Tên khách hàng (để hiển thị trong notification)
     */
    @Column(name = "customer_name", length = 255)
    private String customerName;

    /**
     * Trạng thái mới của video (optional)
     */
    @Column(name = "new_status", length = 50)
    private String newStatus;

    /**
     * Trạng thái cũ của video (optional)
     */
    @Column(name = "old_status", length = 50)
    private String oldStatus;

    /**
     * Đánh dấu notification đã được đọc hay chưa
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;


    /**
     * Thời gian notification được đọc (null nếu chưa đọc)
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * Tenant ID cho multi-tenant support
     */
    @Column(name = "tenant_id", length = 50)
    private String tenantId;


    /**
     * Helper method để đánh dấu notification đã đọc
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    /**
     * Helper method để check notification có phải unread không
     */
    public boolean isUnread() {
        return !Boolean.TRUE.equals(this.isRead);
    }
}