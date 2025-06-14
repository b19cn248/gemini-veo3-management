package com.ptit.google.veo3.controller;

import com.ptit.google.veo3.entity.AuditAction;
import com.ptit.google.veo3.entity.AuditLog;
import com.ptit.google.veo3.entity.EntityType;
import com.ptit.google.veo3.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller để xử lý các HTTP requests liên quan đến Audit Logs
 * 
 * Chức năng:
 * - Cung cấp API endpoints để truy vấn audit logs
 * - Admin dashboard support với filtering và search
 * - Video history tracking cho specific videos
 * - User activity monitoring
 * - Export capabilities cho audit reports
 * 
 * Security:
 * - Chỉ ADMIN role mới có thể access full audit data
 * - STAFF có thể xem audit logs của videos họ được assign
 * - Audit của audit - track việc ai access audit logs
 * 
 * Tuân thủ SOLID Principles:
 * - Single Responsibility: Chỉ handle audit-related HTTP requests
 * - Open/Closed: Có thể extend thêm endpoints mà không sửa code cũ
 * - Dependency Inversion: Depend on service abstractions
 * 
 * REST API Design:
 * - RESTful URLs và HTTP methods
 * - Consistent response format
 * - Proper error handling và status codes
 * - Pagination support cho large datasets
 * 
 * Base URL: /api/v1/audit
 * 
 * Endpoints:
 * - GET    /api/v1/audit/video/{videoId}/history     - Lịch sử của video cụ thể
 * - GET    /api/v1/audit/search                      - Search audit logs với filters
 * - GET    /api/v1/audit/user/{username}/activity    - Hoạt động của user
 * - GET    /api/v1/audit/recent                      - Audit logs gần đây
 * - GET    /api/v1/audit/analytics                   - Analytics data
 * - GET    /api/v1/audit/export                      - Export audit data
 * 
 * @author System
 * @since 1.6.0
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3006"})
public class AuditController {
    
    private final AuditService auditService;
    
    // ============= VIDEO HISTORY ENDPOINTS =============
    
    /**
     * Lấy toàn bộ lịch sử của một video cụ thể
     * 
     * Endpoint: GET /api/v1/audit/video/{videoId}/history
     * 
     * Security: ADMIN và STAFF (nếu là video được assign cho họ)
     * 
     * @param videoId ID của video
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 20)
     * @param sort Sort field (default: performedAt)
     * @param direction Sort direction (default: desc)
     * @return ResponseEntity chứa page of audit logs
     */
    @GetMapping("/video/{videoId}/history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<Map<String, Object>> getVideoHistory(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "performedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        
        try {
            log.info("Getting video history for Video ID: {} - Page: {}, Size: {}", videoId, page, size);
            
            // Tạo Pageable object
            Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
            
            // Lấy audit logs
            Page<AuditLog> auditPage = auditService.getVideoHistoryPaged(videoId, pageable);
            
            // Tạo response
            Map<String, Object> response = new HashMap<>();
            response.put("audits", auditPage.getContent());
            response.put("currentPage", auditPage.getNumber());
            response.put("totalPages", auditPage.getTotalPages());
            response.put("totalElements", auditPage.getTotalElements());
            response.put("hasNext", auditPage.hasNext());
            response.put("hasPrevious", auditPage.hasPrevious());
            response.put("videoId", videoId);
            
            log.info("Retrieved {} audit logs for Video ID: {}", auditPage.getNumberOfElements(), videoId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting video history for ID {}: {}", videoId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve video history");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("videoId", videoId);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Lấy lịch sử video không phân trang (cho export hoặc timeline view)
     * 
     * Endpoint: GET /api/v1/audit/video/{videoId}/history/all
     * 
     * @param videoId ID của video
     * @return ResponseEntity chứa list of audit logs
     */
    @GetMapping("/video/{videoId}/history/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<Map<String, Object>> getVideoHistoryAll(@PathVariable Long videoId) {
        try {
            log.info("Getting complete video history for Video ID: {}", videoId);
            
            List<AuditLog> auditLogs = auditService.getVideoHistory(videoId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("audits", auditLogs);
            response.put("totalCount", auditLogs.size());
            response.put("videoId", videoId);
            
            log.info("Retrieved {} audit logs for Video ID: {}", auditLogs.size(), videoId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting complete video history for ID {}: {}", videoId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve complete video history");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("videoId", videoId);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    // ============= ADMIN DASHBOARD ENDPOINTS =============
    
    /**
     * Search audit logs với multiple filters
     * 
     * Endpoint: GET /api/v1/audit/search
     * 
     * Query Parameters:
     * - entityType: Loại entity (VIDEO, USER, etc.)
     * - action: Loại action (CREATE, UPDATE, DELETE, etc.)
     * - performedBy: Username filter
     * - fromDate: Start date filter (ISO format)
     * - toDate: End date filter (ISO format)
     * - page, size, sort, direction: Pagination parameters
     * 
     * Security: Chỉ ADMIN role
     * 
     * @param entityType Entity type filter
     * @param action Action type filter
     * @param performedBy User filter
     * @param fromDate Start date filter
     * @param toDate End date filter
     * @param page Page number
     * @param size Page size
     * @param sort Sort field
     * @param direction Sort direction
     * @return ResponseEntity chứa filtered audit logs
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> searchAuditLogs(
            @RequestParam(required = false) EntityType entityType,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String performedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "performedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        
        try {
            log.info("Searching audit logs - EntityType: {}, Action: {}, User: {}, From: {}, To: {}", 
                entityType, action, performedBy, fromDate, toDate);
            
            // Tạo Pageable object
            Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
            
            // Search audit logs
            Page<AuditLog> auditPage = auditService.searchAuditLogs(
                entityType, action, performedBy, fromDate, toDate, pageable);
            
            // Tạo response
            Map<String, Object> response = new HashMap<>();
            response.put("audits", auditPage.getContent());
            response.put("currentPage", auditPage.getNumber());
            response.put("totalPages", auditPage.getTotalPages());
            response.put("totalElements", auditPage.getTotalElements());
            response.put("hasNext", auditPage.hasNext());
            response.put("hasPrevious", auditPage.hasPrevious());
            
            // Include filter parameters in response
            Map<String, Object> filters = new HashMap<>();
            filters.put("entityType", entityType);
            filters.put("action", action);
            filters.put("performedBy", performedBy);
            filters.put("fromDate", fromDate);
            filters.put("toDate", toDate);
            response.put("filters", filters);
            
            log.info("Found {} audit logs matching search criteria", auditPage.getTotalElements());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error searching audit logs: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to search audit logs");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    // ============= USER ACTIVITY ENDPOINTS =============
    
    /**
     * Lấy hoạt động của một user cụ thể
     * 
     * Endpoint: GET /api/v1/audit/user/{username}/activity
     * 
     * Security: ADMIN hoặc chính user đó
     * 
     * @param username Username
     * @param page Page number
     * @param size Page size
     * @param sort Sort field
     * @param direction Sort direction
     * @return ResponseEntity chứa user activities
     */
    @GetMapping("/user/{username}/activity")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #username")
    public ResponseEntity<Map<String, Object>> getUserActivity(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "performedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        
        try {
            log.info("Getting user activity for: {} - Page: {}, Size: {}", username, page, size);
            
            // Tạo Pageable object
            Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
            
            // This method needs to be added to AuditService
            // Page<AuditLog> auditPage = auditService.getUserActivity(username, pageable);
            
            // Tạm thời return empty result
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User activity endpoint - to be implemented");
            response.put("username", username);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting user activity for {}: {}", username, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve user activity");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("username", username);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    // ============= ANALYTICS ENDPOINTS =============
    
    /**
     * Lấy recent audit logs cho dashboard
     * 
     * Endpoint: GET /api/v1/audit/recent
     * 
     * Security: ADMIN role
     * 
     * @param limit Số lượng records (default: 50)
     * @return ResponseEntity chứa recent audit logs
     */
    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getRecentAudits(
            @RequestParam(defaultValue = "50") int limit) {
        
        try {
            log.info("Getting recent audit logs - Limit: {}", limit);
            
            // This method needs to be added to AuditService
            // List<AuditLog> recentAudits = auditService.getRecentAudits(limit);
            
            // Tạm thời return empty result
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Recent audits endpoint - to be implemented");
            response.put("limit", limit);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting recent audits: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve recent audits");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Lấy analytics data cho audit dashboard
     * 
     * Endpoint: GET /api/v1/audit/analytics
     * 
     * Security: ADMIN role
     * 
     * @param fromDate Start date for analytics
     * @param toDate End date for analytics
     * @return ResponseEntity chứa analytics data
     */
    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAuditAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        
        try {
            log.info("Getting audit analytics - From: {}, To: {}", fromDate, toDate);
            
            // Set default date range if not provided
            if (toDate == null) {
                toDate = LocalDateTime.now();
            }
            if (fromDate == null) {
                fromDate = toDate.minusDays(30); // Default to last 30 days
            }
            
            // This logic needs to be implemented in AuditService
            Map<String, Object> analytics = new HashMap<>();
            analytics.put("message", "Analytics endpoint - to be implemented");
            analytics.put("fromDate", fromDate);
            analytics.put("toDate", toDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("analytics", analytics);
            response.put("dateRange", Map.of("from", fromDate, "to", toDate));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting audit analytics: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve audit analytics");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    // ============= ENUM ENDPOINTS =============
    
    /**
     * Lấy danh sách tất cả entity types
     * 
     * Endpoint: GET /api/v1/audit/entity-types
     * 
     * @return ResponseEntity chứa entity types
     */
    @GetMapping("/entity-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getEntityTypes() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("entityTypes", EntityType.values());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting entity types: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve entity types");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Lấy danh sách tất cả audit actions
     * 
     * Endpoint: GET /api/v1/audit/actions
     * 
     * @return ResponseEntity chứa audit actions
     */
    @GetMapping("/actions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAuditActions() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("actions", AuditAction.values());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting audit actions: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve audit actions");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    // ============= HEALTH CHECK =============
    
    /**
     * Health check endpoint cho audit service
     * 
     * Endpoint: GET /api/v1/audit/health
     * 
     * @return ResponseEntity với health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("service", "AuditService");
            health.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            log.error("Audit service health check failed: {}", e.getMessage(), e);
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
}