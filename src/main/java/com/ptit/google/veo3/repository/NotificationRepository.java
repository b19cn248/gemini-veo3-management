package com.ptit.google.veo3.repository;

import com.ptit.google.veo3.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface cho Notification entity
 * 
 * Cung cấp các method để:
 * - Query notifications theo user và criteria
 * - Đánh dấu notifications đã đọc
 * - Lấy số lượng unread notifications
 * - Soft delete notifications
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Lấy danh sách notifications của một user với phân trang
     * Chỉ lấy notifications chưa bị xóa (soft delete)
     * 
     * @param recipientUsername Username của người nhận
     * @param pageable Phân trang và sắp xếp
     * @return Page của notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.recipientUsername = :recipientUsername " +
           "AND n.isDeleted = false ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientUsernameAndNotDeleted(
            @Param("recipientUsername") String recipientUsername, 
            Pageable pageable);

    /**
     * Lấy danh sách notifications của user theo trạng thái đọc
     * 
     * @param recipientUsername Username của người nhận
     * @param isRead Trạng thái đã đọc/chưa đọc
     * @param pageable Phân trang và sắp xếp
     * @return Page của notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.recipientUsername = :recipientUsername " +
           "AND n.isRead = :isRead AND n.isDeleted = false ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientUsernameAndIsReadAndNotDeleted(
            @Param("recipientUsername") String recipientUsername,
            @Param("isRead") Boolean isRead,
            Pageable pageable);

    /**
     * Đếm số notifications chưa đọc của user
     * 
     * @param recipientUsername Username của người nhận
     * @return Số lượng notifications chưa đọc
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipientUsername = :recipientUsername " +
           "AND n.isRead = false AND n.isDeleted = false")
    Long countUnreadNotifications(@Param("recipientUsername") String recipientUsername);

    /**
     * Tìm notification theo ID và recipient (để đảm bảo security)
     * 
     * @param id ID của notification
     * @param recipientUsername Username của người nhận
     * @return Optional notification
     */
    @Query("SELECT n FROM Notification n WHERE n.id = :id " +
           "AND n.recipientUsername = :recipientUsername AND n.isDeleted = false")
    Optional<Notification> findByIdAndRecipientUsername(
            @Param("id") Long id, 
            @Param("recipientUsername") String recipientUsername);

    /**
     * Đánh dấu notification đã đọc
     * 
     * @param id ID của notification
     * @param recipientUsername Username để verify ownership
     * @param readAt Thời gian đọc
     * @return Số record được update
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt " +
           "WHERE n.id = :id AND n.recipientUsername = :recipientUsername AND n.isDeleted = false")
    int markAsRead(@Param("id") Long id, 
                   @Param("recipientUsername") String recipientUsername,
                   @Param("readAt") LocalDateTime readAt);

    /**
     * Đánh dấu tất cả notifications của user đã đọc
     * 
     * @param recipientUsername Username của người nhận
     * @param readAt Thời gian đọc
     * @return Số record được update
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt " +
           "WHERE n.recipientUsername = :recipientUsername AND n.isRead = false AND n.isDeleted = false")
    int markAllAsRead(@Param("recipientUsername") String recipientUsername,
                      @Param("readAt") LocalDateTime readAt);

    /**
     * Soft delete notification
     * 
     * @param id ID của notification
     * @param recipientUsername Username để verify ownership
     * @return Số record được update
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isDeleted = true " +
           "WHERE n.id = :id AND n.recipientUsername = :recipientUsername")
    int softDeleteNotification(@Param("id") Long id, 
                               @Param("recipientUsername") String recipientUsername);

    /**
     * Lấy notifications liên quan đến một video cụ thể
     * 
     * @param videoId ID của video
     * @return List notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.videoId = :videoId " +
           "AND n.isDeleted = false ORDER BY n.createdAt DESC")
    List<Notification> findByVideoIdAndNotDeleted(@Param("videoId") Long videoId);

    /**
     * Cleanup old notifications (có thể dùng cho scheduled job)
     * 
     * @param cutoffDate Ngày cutoff - xóa notifications cũ hơn ngày này
     * @return Số record được xóa
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isDeleted = true " +
           "WHERE n.createdAt < :cutoffDate AND n.isDeleted = false")
    int deleteOldNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Lấy notifications mới nhất của user (không phân trang)
     * 
     * @param recipientUsername Username của người nhận
     * @param limit Số lượng tối đa
     * @return List notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.recipientUsername = :recipientUsername " +
           "AND n.isDeleted = false ORDER BY n.createdAt DESC LIMIT :limit")
    List<Notification> findRecentNotifications(
            @Param("recipientUsername") String recipientUsername,
            @Param("limit") int limit);
}