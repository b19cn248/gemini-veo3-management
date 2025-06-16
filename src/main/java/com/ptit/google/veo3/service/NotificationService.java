package com.ptit.google.veo3.service;

import com.ptit.google.veo3.dto.NotificationDto;
import com.ptit.google.veo3.dto.NotificationResponseDto;
import com.ptit.google.veo3.dto.NotificationType;
import com.ptit.google.veo3.entity.Notification;
import com.ptit.google.veo3.entity.Video;
import com.ptit.google.veo3.multitenant.TenantContext;
import com.ptit.google.veo3.repository.NotificationRepository;
import com.ptit.google.veo3.service.interfaces.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service xử lý gửi real-time notifications qua WebSocket
 * 
 * Service này chịu trách nhiệm:
 * - Tạo notification message với đầy đủ thông tin
 * - Gửi notification đến đúng user thông qua WebSocket
 * - Log notification activities cho debugging
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService implements INotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;

    /**
     * Gửi notification khi video cần sửa gấp
     * 
     * @param video Video cần sửa gấp
     * @param sender Người yêu cầu sửa (admin/sale)
     */
    @Override
    @Transactional
    public void sendUrgentFixNotification(Video video, String sender) {
        if (video.getAssignedStaff() == null || video.getAssignedStaff().trim().isEmpty()) {
            log.warn("Cannot send urgent fix notification for video ID {} - no assigned staff", video.getId());
            return;
        }

        String recipient = video.getAssignedStaff().trim();
        
        NotificationDto notification = NotificationDto.builder()
                .videoId(video.getId())
                .type(NotificationType.VIDEO_NEEDS_URGENT_FIX)
                .title("Video cần sửa gấp!")
                .message(String.format("Video của khách hàng '%s' cần được sửa gấp. Vui lòng kiểm tra và xử lý ngay.", 
                        video.getCustomerName()))
                .customerName(video.getCustomerName())
                .sender(sender)
                .recipient(recipient)
                .timestamp(LocalDateTime.now())
                .newStatus("CAN_SUA_GAP")
                .oldStatus(video.getDeliveryStatus() != null ? video.getDeliveryStatus().name() : null)
                .build();

        // Lưu vào database
        Notification savedNotification = saveNotificationFromDto(notification);
        
        // Gửi real-time notification
        sendNotificationToUser(recipient, notification);
        
        log.info("Sent urgent fix notification for video ID {} to staff '{}' from '{}', saved with ID {}", 
                video.getId(), recipient, sender, savedNotification.getId());
    }

    /**
     * Gửi notification khi video đã sửa xong
     * 
     * @param video Video đã được sửa xong
     * @param staff Nhân viên đã sửa xong
     */
    @Override
    @Transactional
    public void sendFixCompletedNotification(Video video, String staff) {
        if (video.getCreatedBy() == null || video.getCreatedBy().trim().isEmpty()) {
            log.warn("Cannot send fix completed notification for video ID {} - no creator info", video.getId());
            return;
        }

        String recipient = video.getCreatedBy().trim();
        
        NotificationDto notification = NotificationDto.builder()
                .videoId(video.getId())
                .type(NotificationType.VIDEO_FIXED_COMPLETED)
                .title("Video đã được sửa xong!")
                .message(String.format("Nhân viên '%s' đã hoàn thành sửa video của khách hàng '%s'. Video đã sẵn sàng để kiểm tra.", 
                        staff, video.getCustomerName()))
                .customerName(video.getCustomerName())
                .sender(staff)
                .recipient(recipient)
                .timestamp(LocalDateTime.now())
                .newStatus("DA_SUA_XONG")
                .oldStatus(video.getStatus() != null ? video.getStatus().name() : null)
                .build();

        // Lưu vào database
        Notification savedNotification = saveNotificationFromDto(notification);
        
        // Gửi real-time notification
        sendNotificationToUser(recipient, notification);
        
        log.info("Sent fix completed notification for video ID {} to creator '{}' from staff '{}', saved with ID {}", 
                video.getId(), recipient, staff, savedNotification.getId());
    }

    /**
     * Gửi notification đến specific user thông qua WebSocket
     * 
     * @param username Tên user nhận notification
     * @param notification Notification data
     */
    private void sendNotificationToUser(String username, NotificationDto notification) {
        try {
            if (!StringUtils.hasText(username)) {
                log.warn("Cannot send notification - username is empty");
                return;
            }

            // Destination pattern: /user/{username}/notifications
            String destination = "/user/" + username + "/notifications";
            
            messagingTemplate.convertAndSendToUser(
                    username, 
                    "/notifications", 
                    notification
            );
            
            log.debug("Notification sent to destination: {} for user: {}", destination, username);
            
        } catch (Exception e) {
            log.error("Error sending notification to user '{}': ", username, e);
        }
    }

    /**
     * Test method để gửi notification (có thể dùng cho debugging)
     * 
     * @param username Target username
     * @param message Test message
     */
    @Override
    public void sendTestNotification(String username, String message) {
        NotificationDto testNotification = NotificationDto.builder()
                .type(NotificationType.VIDEO_NEEDS_URGENT_FIX)
                .title("Test Notification")
                .message(message)
                .sender("SYSTEM")
                .recipient(username)
                .timestamp(LocalDateTime.now())
                .build();

        sendNotificationToUser(username, testNotification);
        log.info("Sent test notification to user: {}", username);
    }

    // ================ DATABASE OPERATIONS ================

    @Override
    @Transactional
    public Notification saveNotification(Notification notification) {
        if (notification.getTenantId() == null) {
            notification.setTenantId(TenantContext.getTenantId());
        }
        return notificationRepository.save(notification);
    }

    @Override
    public Page<NotificationResponseDto> getNotificationsByUser(
            String username, 
            int page, 
            int size, 
            String sortBy, 
            String sortDirection,
            Boolean isRead) {
        
        // Tạo Pageable với sorting
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        // Query với hoặc không filter theo isRead
        Page<Notification> notificationPage;
        if (isRead != null) {
            notificationPage = notificationRepository.findByRecipientUsernameAndIsReadAndNotDeleted(
                    username, isRead, pageable);
        } else {
            notificationPage = notificationRepository.findByRecipientUsernameAndNotDeleted(
                    username, pageable);
        }
        
        // Convert sang DTO
        return notificationPage.map(this::mapToResponseDto);
    }

    @Override
    @Transactional
    public NotificationResponseDto markAsRead(Long notificationId, String username) {
        Notification notification = notificationRepository.findByIdAndRecipientUsername(notificationId, username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy notification với ID " + notificationId + " cho user " + username));
        
        if (!notification.getIsRead()) {
            notification.markAsRead();
            notificationRepository.save(notification);
            log.info("Marked notification {} as read for user {}", notificationId, username);
        }
        
        return mapToResponseDto(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(String username) {
        int updatedCount = notificationRepository.markAllAsRead(username, LocalDateTime.now());
        log.info("Marked {} notifications as read for user {}", updatedCount, username);
        return updatedCount;
    }

    @Override
    public Long getUnreadCount(String username) {
        return notificationRepository.countUnreadNotifications(username);
    }

    @Override
    @Transactional
    public boolean deleteNotification(Long notificationId, String username) {
        int deletedCount = notificationRepository.softDeleteNotification(notificationId, username);
        if (deletedCount > 0) {
            log.info("Soft deleted notification {} for user {}", notificationId, username);
            return true;
        } else {
            throw new IllegalArgumentException(
                    "Không tìm thấy notification với ID " + notificationId + " cho user " + username);
        }
    }

    @Override
    public NotificationResponseDto getNotificationById(Long notificationId, String username) {
        Notification notification = notificationRepository.findByIdAndRecipientUsername(notificationId, username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy notification với ID " + notificationId + " cho user " + username));
        
        return mapToResponseDto(notification);
    }

    @Override
    public List<NotificationResponseDto> getNotificationsByVideo(Long videoId) {
        List<Notification> notifications = notificationRepository.findByVideoIdAndNotDeleted(videoId);
        return notifications.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationResponseDto> getRecentNotifications(String username, int limit) {
        List<Notification> notifications = notificationRepository.findRecentNotifications(username, limit);
        return notifications.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int cleanupOldNotifications(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        int deletedCount = notificationRepository.deleteOldNotifications(cutoffDate);
        log.info("Cleaned up {} old notifications (older than {} days)", deletedCount, daysToKeep);
        return deletedCount;
    }

    // ================ HELPER METHODS ================

    /**
     * Helper method để lưu notification từ DTO
     */
    private Notification saveNotificationFromDto(NotificationDto dto) {
        Notification notification = Notification.builder()
                .videoId(dto.getVideoId())
                .recipientUsername(dto.getRecipient())
                .senderUsername(dto.getSender())
                .notificationType(dto.getType())
                .title(dto.getTitle())
                .message(dto.getMessage())
                .customerName(dto.getCustomerName())
                .newStatus(dto.getNewStatus())
                .oldStatus(dto.getOldStatus())
                .isRead(false)
                .tenantId(TenantContext.getTenantId())
                .build();
        
        return saveNotification(notification);
    }

    /**
     * Helper method để convert entity sang response DTO
     */
    private NotificationResponseDto mapToResponseDto(Notification notification) {
        return NotificationResponseDto.builder()
                .id(notification.getId())
                .videoId(notification.getVideoId())
                .type(notification.getNotificationType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .customerName(notification.getCustomerName())
                .sender(notification.getSenderUsername())
                .recipient(notification.getRecipientUsername())
                .newStatus(notification.getNewStatus())
                .oldStatus(notification.getOldStatus())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .tenantId(notification.getTenantId())
                .build();
    }
}