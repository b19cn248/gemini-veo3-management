package com.ptit.google.veo3.repository;

import com.ptit.google.veo3.entity.AuditAction;
import com.ptit.google.veo3.entity.AuditLog;
import com.ptit.google.veo3.entity.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface cho AuditLog entity
 * 
 * Chức năng: Provide data access layer cho audit logs với performance optimization
 * 
 * Tuân thủ SOLID Principles:
 * - Interface Segregation: Chỉ define methods cần thiết cho audit operations
 * - Dependency Inversion: Depend on Spring Data abstraction
 * 
 * Performance Strategy:
 * - Sử dụng native queries cho complex operations
 * - Pagination support cho large datasets
 * - Proper indexing strategy (defined in Liquibase)
 * - Query projections để giảm memory usage
 * 
 * Security Considerations:
 * - Read-only operations (audit logs không bao giờ được update/delete)
 * - Tenant-aware queries cho multi-tenant support
 * - Parameter validation để tránh SQL injection
 * 
 * @author System
 * @since 1.6.0
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    // ============= VIDEO HISTORY QUERIES =============
    
    /**
     * Lấy toàn bộ lịch sử của một video cụ thể
     * Sắp xếp theo thời gian mới nhất đầu tiên
     * 
     * Performance: Sử dụng composite index (entity_name, entity_id, performed_at)
     * 
     * @param videoId ID của video
     * @return List audit logs của video
     */
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE a.entityType = com.ptit.google.veo3.entity.EntityType.VIDEO 
        AND a.entityId = :videoId 
        ORDER BY a.performedAt DESC
        """)
    List<AuditLog> findVideoHistory(@Param("videoId") Long videoId);
    
    /**
     * Lấy lịch sử video với pagination cho large datasets
     * 
     * @param videoId ID của video
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE a.entityType = com.ptit.google.veo3.entity.EntityType.VIDEO 
        AND a.entityId = :videoId 
        ORDER BY a.performedAt DESC
        """)
    Page<AuditLog> findVideoHistoryPaged(@Param("videoId") Long videoId, Pageable pageable);
    
    /**
     * Lấy lịch sử video trong khoảng thời gian cụ thể
     * Hữu ích cho report và analysis
     * 
     * @param videoId ID của video
     * @param fromDate Thời điểm bắt đầu
     * @param toDate Thời điểm kết thúc
     * @return List audit logs trong khoảng thời gian
     */
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE a.entityType = com.ptit.google.veo3.entity.EntityType.VIDEO 
        AND a.entityId = :videoId 
        AND a.performedAt BETWEEN :fromDate AND :toDate 
        ORDER BY a.performedAt DESC
        """)
    List<AuditLog> findVideoHistoryByDateRange(
        @Param("videoId") Long videoId,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate
    );
    
    // ============= ADMIN DASHBOARD QUERIES =============
    
    /**
     * Tìm audit logs theo multiple criteria với pagination
     * Dành cho admin dashboard với filtering
     * 
     * @param entityType Loại entity (nullable)
     * @param action Loại action (nullable) 
     * @param performedBy Username (nullable)
     * @param fromDate Thời điểm bắt đầu (nullable)
     * @param toDate Thời điểm kết thúc (nullable)
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE (:entityType IS NULL OR a.entityType = :entityType)
        AND (:action IS NULL OR a.action = :action)
        AND (:performedBy IS NULL OR LOWER(a.performedBy) LIKE LOWER(CONCAT('%', :performedBy, '%')))
        AND (:fromDate IS NULL OR a.performedAt >= :fromDate)
        AND (:toDate IS NULL OR a.performedAt <= :toDate)
        ORDER BY a.performedAt DESC
        """)
    Page<AuditLog> findAuditLogsByFilters(
        @Param("entityType") EntityType entityType,
        @Param("action") AuditAction action,
        @Param("performedBy") String performedBy,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate,
        Pageable pageable
    );
    
    /**
     * Lấy audit logs của tất cả videos
     * Dành cho admin overview
     * 
     * @param pageable Pagination parameters
     * @return Page of video audit logs
     */
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE a.entityType = com.ptit.google.veo3.entity.EntityType.VIDEO 
        ORDER BY a.performedAt DESC
        """)
    Page<AuditLog> findAllVideoAudits(Pageable pageable);
    
    // ============= USER ACTIVITY QUERIES =============
    
    /**
     * Lấy hoạt động của một user cụ thể
     * Dành cho user activity tracking
     * 
     * @param username Username
     * @param pageable Pagination parameters
     * @return Page of user's audit logs
     */
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE LOWER(a.performedBy) = LOWER(:username)
        ORDER BY a.performedAt DESC
        """)
    Page<AuditLog> findUserActivity(@Param("username") String username, Pageable pageable);
    
    /**
     * Lấy hoạt động của user trong khoảng thời gian
     * 
     * @param username Username
     * @param fromDate Thời điểm bắt đầu
     * @param toDate Thời điểm kết thúc
     * @param pageable Pagination parameters
     * @return Page of user's audit logs
     */
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE LOWER(a.performedBy) = LOWER(:username)
        AND a.performedAt BETWEEN :fromDate AND :toDate 
        ORDER BY a.performedAt DESC
        """)
    Page<AuditLog> findUserActivityByDateRange(
        @Param("username") String username,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate,
        Pageable pageable
    );
    
    // ============= ANALYTICS QUERIES =============
    
    /**
     * Đếm số lượng audit logs theo action type
     * Dành cho analytics và reporting
     * 
     * @param fromDate Thời điểm bắt đầu
     * @param toDate Thời điểm kết thúc
     * @return Map với action và count
     */
    @Query("""
        SELECT a.action as action, COUNT(a) as count 
        FROM AuditLog a 
        WHERE a.performedAt BETWEEN :fromDate AND :toDate 
        GROUP BY a.action 
        ORDER BY COUNT(a) DESC
        """)
    List<Object[]> countActionsByDateRange(
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate
    );
    
    /**
     * Đếm số lượng hoạt động theo user
     * 
     * @param fromDate Thời điểm bắt đầu
     * @param toDate Thời điểm kết thúc
     * @return List với username và count
     */
    @Query("""
        SELECT a.performedBy as username, COUNT(a) as count 
        FROM AuditLog a 
        WHERE a.performedAt BETWEEN :fromDate AND :toDate 
        GROUP BY a.performedBy 
        ORDER BY COUNT(a) DESC
        """)
    List<Object[]> countActivitiesByUser(
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate
    );
    
    /**
     * Lấy audit logs gần đây nhất
     * Dành cho dashboard recent activities
     * 
     * @param limit Số lượng records
     * @return List audit logs gần đây
     */
    @Query("""
        SELECT a FROM AuditLog a 
        ORDER BY a.performedAt DESC 
        LIMIT :limit
        """)
    List<AuditLog> findRecentAudits(@Param("limit") int limit);
    
    // ============= ENTITY LIFECYCLE QUERIES =============
    
    /**
     * Tìm audit log CREATE của một entity
     * Để xác định ai tạo entity và khi nào
     * 
     * @param entityType Loại entity
     * @param entityId ID của entity
     * @return Audit log CREATE (có thể null)
     */
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE a.entityType = :entityType 
        AND a.entityId = :entityId 
        AND a.action = com.ptit.google.veo3.entity.AuditAction.CREATE
        ORDER BY a.performedAt ASC
        """)
    AuditLog findEntityCreationAudit(
        @Param("entityType") EntityType entityType, 
        @Param("entityId") Long entityId
    );
    
    /**
     * Tìm audit log DELETE của một entity
     * Để xác định ai xóa entity và khi nào
     * 
     * @param entityType Loại entity
     * @param entityId ID của entity
     * @return Audit log DELETE (có thể null)
     */
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE a.entityType = :entityType 
        AND a.entityId = :entityId 
        AND a.action = com.ptit.google.veo3.entity.AuditAction.DELETE
        ORDER BY a.performedAt DESC
        """)
    AuditLog findEntityDeletionAudit(
        @Param("entityType") EntityType entityType, 
        @Param("entityId") Long entityId
    );
    
    // ============= BUSINESS LOGIC QUERIES =============
    
    /**
     * Lấy tất cả staff assignments cho video
     * Để track ai đã được assign và unassign
     * 
     * @param videoId ID của video
     * @return List audit logs về staff assignment
     */
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE a.entityType = com.ptit.google.veo3.entity.EntityType.VIDEO 
        AND a.entityId = :videoId 
        AND a.action IN (
            com.ptit.google.veo3.entity.AuditAction.ASSIGN_STAFF, 
            com.ptit.google.veo3.entity.AuditAction.UNASSIGN_STAFF
        )
        ORDER BY a.performedAt ASC
        """)
    List<AuditLog> findVideoStaffAssignments(@Param("videoId") Long videoId);
    
    /**
     * Lấy tất cả status changes của video
     * Để track video workflow progression
     * 
     * @param videoId ID của video
     * @return List audit logs về status changes
     */
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE a.entityType = com.ptit.google.veo3.entity.EntityType.VIDEO 
        AND a.entityId = :videoId 
        AND a.action = com.ptit.google.veo3.entity.AuditAction.UPDATE_STATUS
        ORDER BY a.performedAt ASC
        """)
    List<AuditLog> findVideoStatusChanges(@Param("videoId") Long videoId);
    
    // ============= SECURITY & COMPLIANCE QUERIES =============
    
    /**
     * Tìm audit logs từ IP address cụ thể
     * Dành cho security investigation
     * 
     * @param ipAddress IP address
     * @param fromDate Thời điểm bắt đầu
     * @param toDate Thời điểm kết thúc
     * @return List audit logs từ IP
     */
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE a.ipAddress = :ipAddress
        AND a.performedAt BETWEEN :fromDate AND :toDate 
        ORDER BY a.performedAt DESC
        """)
    List<AuditLog> findAuditsByIpAddress(
        @Param("ipAddress") String ipAddress,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate
    );
    
    /**
     * Kiểm tra có audit logs nào trong khoảng thời gian không
     * Dành cho data retention và cleanup
     * 
     * @param beforeDate Ngày cutoff
     * @return true nếu có data cũ hơn beforeDate
     */
    @Query("""
        SELECT COUNT(a) > 0 FROM AuditLog a 
        WHERE a.performedAt < :beforeDate
        """)
    boolean existsAuditsBefore(@Param("beforeDate") LocalDateTime beforeDate);
    
    // ============= MULTI-TENANT SUPPORT =============
    
    /**
     * Tìm audit logs theo tenant (nếu sử dụng multi-tenant)
     * 
     * @param tenantId Tenant ID
     * @param pageable Pagination parameters
     * @return Page of tenant's audit logs
     */
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE (:tenantId IS NULL OR a.tenantId = :tenantId)
        ORDER BY a.performedAt DESC
        """)
    Page<AuditLog> findAuditsByTenant(@Param("tenantId") String tenantId, Pageable pageable);
}