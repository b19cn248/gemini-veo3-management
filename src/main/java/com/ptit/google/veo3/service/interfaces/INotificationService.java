package com.ptit.google.veo3.service.interfaces;

import com.ptit.google.veo3.dto.NotificationResponseDto;
import com.ptit.google.veo3.entity.Notification;
import com.ptit.google.veo3.entity.Video;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Interface định nghĩa contract cho NotificationService
 * 
 * Cung cấp các operations:
 * - Real-time notifications (WebSocket)
 * - CRUD operations cho notifications
 * - Query notifications với filtering và pagination
 * - Mark as read functionality
 */
public interface INotificationService {

    // ================ REAL-TIME NOTIFICATION METHODS ================
    
    /**
     * Gửi notification khi video cần sửa gấp
     * 
     * @param video Video cần sửa gấp
     * @param sender Người yêu cầu sửa (admin/sale)
     */
    void sendUrgentFixNotification(Video video, String sender);

    /**
     * Gửi notification khi video đã sửa xong
     * 
     * @param video Video đã được sửa xong
     * @param staff Nhân viên đã sửa xong
     */
    void sendFixCompletedNotification(Video video, String staff);

    /**
     * Gửi test notification (dùng cho debugging)
     * 
     * @param username Target username
     * @param message Test message
     */
    void sendTestNotification(String username, String message);

    // ================ DATABASE OPERATIONS ================

    /**
     * Lưu notification vào database
     * 
     * @param notification Notification entity
     * @return Saved notification
     */
    Notification saveNotification(Notification notification);

    /**
     * Lấy danh sách notifications của user với phân trang
     * 
     * @param username Username của người nhận
     * @param page Số trang (0-based)
     * @param size Kích thước trang
     * @param sortBy Trường sắp xếp
     * @param sortDirection Hướng sắp xếp (asc/desc)
     * @param isRead Filter theo trạng thái đọc (null = tất cả)
     * @return Page của NotificationResponseDto
     */
    Page<NotificationResponseDto> getNotificationsByUser(
            String username, 
            int page, 
            int size, 
            String sortBy, 
            String sortDirection,
            Boolean isRead);

    /**
     * Đánh dấu notification đã đọc
     * 
     * @param notificationId ID của notification
     * @param username Username để verify ownership
     * @return Updated notification DTO
     * @throws IllegalArgumentException nếu notification không tồn tại hoặc không thuộc về user
     */
    NotificationResponseDto markAsRead(Long notificationId, String username);

    /**
     * Đánh dấu tất cả notifications của user đã đọc
     * 
     * @param username Username của người nhận
     * @return Số notifications được đánh dấu đã đọc
     */
    int markAllAsRead(String username);

    /**
     * Lấy số notifications chưa đọc của user
     * 
     * @param username Username của người nhận
     * @return Số lượng notifications chưa đọc
     */
    Long getUnreadCount(String username);

    /**
     * Xóa notification (soft delete)
     * 
     * @param notificationId ID của notification
     * @param username Username để verify ownership
     * @return true nếu xóa thành công
     * @throws IllegalArgumentException nếu notification không tồn tại hoặc không thuộc về user
     */
    boolean deleteNotification(Long notificationId, String username);

    /**
     * Lấy notification chi tiết theo ID
     * 
     * @param notificationId ID của notification
     * @param username Username để verify ownership
     * @return Notification DTO
     * @throws IllegalArgumentException nếu notification không tồn tại hoặc không thuộc về user
     */
    NotificationResponseDto getNotificationById(Long notificationId, String username);

    /**
     * Lấy notifications liên quan đến một video
     * 
     * @param videoId ID của video
     * @return List notification DTOs
     */
    List<NotificationResponseDto> getNotificationsByVideo(Long videoId);

    /**
     * Lấy notifications mới nhất của user (không phân trang)
     * 
     * @param username Username của người nhận
     * @param limit Số lượng tối đa (default = 10)
     * @return List notification DTOs
     */
    List<NotificationResponseDto> getRecentNotifications(String username, int limit);

    // ================ UTILITY METHODS ================

    /**
     * Cleanup notifications cũ (có thể dùng cho scheduled job)
     * 
     * @param daysToKeep Số ngày giữ lại notifications
     * @return Số notifications được xóa
     */
    int cleanupOldNotifications(int daysToKeep);
}