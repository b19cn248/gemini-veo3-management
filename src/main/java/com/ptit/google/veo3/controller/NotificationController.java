package com.ptit.google.veo3.controller;

import com.ptit.google.veo3.dto.NotificationResponseDto;
import com.ptit.google.veo3.service.NotificationService;
import com.ptit.google.veo3.service.interfaces.IJwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket Controller xử lý real-time notifications
 * 
 * Controller này cung cấp:
 * - WebSocket message mapping cho client communication
 * - REST endpoint để test notification system
 * - Connection handling và user session management
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final IJwtTokenService jwtTokenService;

    /**
     * Handle khi client subscribe vào notification channel
     * Message mapping: /app/subscribe-notifications
     * 
     * @param headerAccessor STOMP header accessor để lấy session info
     */
    @MessageMapping("/subscribe-notifications")
    public void subscribeNotifications(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        
        log.info("User '{}' subscribed to notifications (session: {})", username, sessionId);
        
        // Có thể thêm logic lưu user session mapping tại đây nếu cần
        // Ví dụ: sessionService.addUserSession(username, sessionId);
    }

    /**
     * Handle message từ client (có thể dùng cho acknowledgment hoặc status updates)
     * Message mapping: /app/notification-received
     * 
     * @param notificationId ID của notification đã được client nhận
     * @param headerAccessor STOMP header accessor
     */
    @MessageMapping("/notification-received")
    public void handleNotificationReceived(@Payload String notificationId, SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        
        log.debug("User '{}' acknowledged notification: {}", username, notificationId);
        
        // Có thể thêm logic mark notification as read tại đây
        // Ví dụ: notificationService.markAsRead(notificationId, username);
    }

    /**
     * REST endpoint để test notification system
     * GET /api/v1/notifications/test
     * 
     * @param username Target username
     * @param message Test message
     * @return Response message
     */
    @GetMapping("/api/v1/notifications/test")
    @ResponseBody
    public String testNotification(
            @RequestParam String username,
            @RequestParam(defaultValue = "This is a test notification") String message) {
        
        try {
            notificationService.sendTestNotification(username, message);
            log.info("Test notification sent to user: {}", username);
            return String.format("Test notification sent to user '%s' successfully", username);
            
        } catch (Exception e) {
            log.error("Error sending test notification to user '{}': ", username, e);
            return String.format("Error sending test notification to user '%s': %s", username, e.getMessage());
        }
    }

    /**
     * REST endpoint để kiểm tra WebSocket connection status
     * GET /api/v1/notifications/status
     * 
     * @return WebSocket system status
     */
    @GetMapping("/api/v1/notifications/status")
    @ResponseBody
    public String getNotificationSystemStatus() {
        return "WebSocket Notification System is running. " +
               "Connect to /ws endpoint and subscribe to /user/{username}/notifications";
    }

    // ================ REST API ENDPOINTS ================

    /**
     * GET /api/v1/notifications - Lấy danh sách notifications của user hiện tại
     * 
     * @param page Số trang (0-based, default = 0)
     * @param size Kích thước trang (default = 10)
     * @param sortBy Trường sắp xếp (default = createdAt)
     * @param sortDirection Hướng sắp xếp (asc/desc, default = desc)
     * @param isRead Filter theo trạng thái đọc (optional)
     * @return ResponseEntity chứa Page<NotificationResponseDto>
     */
    @GetMapping("/api/v1/notifications")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) Boolean isRead) {
        
        try {
            String currentUser = jwtTokenService.getCurrentUserNameFromJwt();
            
            Page<NotificationResponseDto> notificationPage = notificationService.getNotificationsByUser(
                    currentUser, page, size, sortBy, sortDirection, isRead);
            
            Map<String, Object> response = createSuccessResponse(
                    "Lấy danh sách notifications thành công",
                    notificationPage.getContent()
            );
            
            // Thêm pagination info
            response.put("pagination", Map.of(
                    "currentPage", notificationPage.getNumber(),
                    "totalPages", notificationPage.getTotalPages(),
                    "totalElements", notificationPage.getTotalElements(),
                    "pageSize", notificationPage.getSize(),
                    "hasNext", notificationPage.hasNext(),
                    "hasPrevious", notificationPage.hasPrevious(),
                    "isFirst", notificationPage.isFirst(),
                    "isLast", notificationPage.isLast()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting notifications: ", e);
            return createErrorResponse("Lỗi khi lấy danh sách notifications: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PUT /api/v1/notifications/{id}/read - Đánh dấu notification đã đọc
     * 
     * @param id ID của notification
     * @return ResponseEntity chứa updated notification
     */
    @PutMapping("/api/v1/notifications/{id}/read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markNotificationAsRead(@PathVariable Long id) {
        try {
            String currentUser = jwtTokenService.getCurrentUserNameFromJwt();
            
            NotificationResponseDto updatedNotification = notificationService.markAsRead(id, currentUser);
            
            Map<String, Object> response = createSuccessResponse(
                    "Đánh dấu notification đã đọc thành công",
                    updatedNotification
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid notification ID {} for mark as read: {}", id, e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
            
        } catch (Exception e) {
            log.error("Error marking notification {} as read: ", id, e);
            return createErrorResponse("Lỗi khi đánh dấu notification đã đọc: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PUT /api/v1/notifications/mark-all-read - Đánh dấu tất cả notifications đã đọc
     * 
     * @return ResponseEntity chứa số lượng notifications được đánh dấu
     */
    @PutMapping("/api/v1/notifications/mark-all-read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAllNotificationsAsRead() {
        try {
            String currentUser = jwtTokenService.getCurrentUserNameFromJwt();
            
            int updatedCount = notificationService.markAllAsRead(currentUser);
            
            Map<String, Object> response = createSuccessResponse(
                    String.format("Đã đánh dấu %d notifications đã đọc", updatedCount),
                    Map.of("updatedCount", updatedCount)
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error marking all notifications as read: ", e);
            return createErrorResponse("Lỗi khi đánh dấu tất cả notifications đã đọc: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/v1/notifications/unread-count - Lấy số notifications chưa đọc
     * 
     * @return ResponseEntity chứa unread count
     */
    @GetMapping("/api/v1/notifications/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUnreadNotificationCount() {
        try {
            String currentUser = jwtTokenService.getCurrentUserNameFromJwt();
            
            Long unreadCount = notificationService.getUnreadCount(currentUser);
            
            Map<String, Object> response = createSuccessResponse(
                    "Lấy số notifications chưa đọc thành công",
                    Map.of("unreadCount", unreadCount)
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting unread notification count: ", e);
            return createErrorResponse("Lỗi khi lấy số notifications chưa đọc: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/v1/notifications/{id} - Lấy chi tiết notification
     * 
     * @param id ID của notification
     * @return ResponseEntity chứa notification details
     */
    @GetMapping("/api/v1/notifications/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNotificationById(@PathVariable Long id) {
        try {
            String currentUser = jwtTokenService.getCurrentUserNameFromJwt();
            
            NotificationResponseDto notification = notificationService.getNotificationById(id, currentUser);
            
            Map<String, Object> response = createSuccessResponse(
                    "Lấy chi tiết notification thành công",
                    notification
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid notification ID {} for get details: {}", id, e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
            
        } catch (Exception e) {
            log.error("Error getting notification {} details: ", id, e);
            return createErrorResponse("Lỗi khi lấy chi tiết notification: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * DELETE /api/v1/notifications/{id} - Xóa notification
     * 
     * @param id ID của notification
     * @return ResponseEntity chứa kết quả xóa
     */
    @DeleteMapping("/api/v1/notifications/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable Long id) {
        try {
            String currentUser = jwtTokenService.getCurrentUserNameFromJwt();
            
            boolean deleted = notificationService.deleteNotification(id, currentUser);
            
            if (deleted) {
                Map<String, Object> response = createSuccessResponse(
                        "Xóa notification thành công",
                        Map.of("deletedId", id)
                );
                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Không thể xóa notification", HttpStatus.BAD_REQUEST);
            }
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid notification ID {} for delete: {}", id, e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
            
        } catch (Exception e) {
            log.error("Error deleting notification {}: ", id, e);
            return createErrorResponse("Lỗi khi xóa notification: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/v1/notifications/recent - Lấy notifications gần đây (không phân trang)
     * 
     * @param limit Số lượng tối đa (default = 10)
     * @return ResponseEntity chứa list notifications
     */
    @GetMapping("/api/v1/notifications/recent")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRecentNotifications(
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            String currentUser = jwtTokenService.getCurrentUserNameFromJwt();
            
            List<NotificationResponseDto> notifications = notificationService.getRecentNotifications(currentUser, limit);
            
            Map<String, Object> response = createSuccessResponse(
                    "Lấy notifications gần đây thành công",
                    notifications
            );
            response.put("total", notifications.size());
            response.put("limit", limit);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting recent notifications: ", e);
            return createErrorResponse("Lỗi khi lấy notifications gần đây: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/v1/notifications/video/{videoId} - Lấy notifications của một video
     * 
     * @param videoId ID của video
     * @return ResponseEntity chứa list notifications
     */
    @GetMapping("/api/v1/notifications/video/{videoId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNotificationsByVideo(@PathVariable Long videoId) {
        try {
            List<NotificationResponseDto> notifications = notificationService.getNotificationsByVideo(videoId);
            
            Map<String, Object> response = createSuccessResponse(
                    String.format("Lấy notifications của video %d thành công", videoId),
                    notifications
            );
            response.put("total", notifications.size());
            response.put("videoId", videoId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting notifications for video {}: ", videoId, e);
            return createErrorResponse("Lỗi khi lấy notifications của video: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ================ HELPER METHODS ================

    /**
     * Helper method để tạo success response
     */
    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Helper method để tạo error response
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("data", null);
        response.put("timestamp", System.currentTimeMillis());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        return ResponseEntity.status(status).body(response);
    }
}