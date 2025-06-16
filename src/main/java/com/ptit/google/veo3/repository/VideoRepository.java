package com.ptit.google.veo3.repository;


import com.ptit.google.veo3.dto.StaffSalaryDto;
import com.ptit.google.veo3.dto.SalesSalaryDto;
import com.ptit.google.veo3.dto.SalesSalaryProjection;
import com.ptit.google.veo3.entity.DeliveryStatus;
import com.ptit.google.veo3.entity.PaymentStatus;
import com.ptit.google.veo3.entity.Video;
import com.ptit.google.veo3.entity.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface để truy cập dữ liệu Video
 * Mở rộng JpaRepository để có sẵn các method CRUD cơ bản
 */
@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    /**
     * Tìm video theo tên khách hàng (không phân biệt hoa thường)
     */
    List<Video> findByCustomerNameContainingIgnoreCase(String customerName);

    /**
     * Tìm video theo trạng thái
     */
    List<Video> findByStatus(VideoStatus status);

    /**
     * Tìm video được tạo trong khoảng thời gian
     */
    @Query("SELECT v FROM Video v WHERE v.createdAt BETWEEN :startDate AND :endDate")
    Page<Video> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       Pageable pageable);

    /**
     * Tìm video theo nhân viên được giao
     */
    List<Video> findByAssignedStaffContainingIgnoreCase(String assignedStaff);

    /**
     * Đếm số video theo trạng thái
     */
    long countByStatus(VideoStatus status);


    @Query("SELECT v FROM Video v WHERE v.isDeleted = false " +
            "AND (:videoStatus IS NULL OR v.status = :videoStatus) " +
            "AND (:assignedStaff IS NULL OR v.assignedStaff LIKE %:assignedStaff%) " +
            "AND (:deliveryStatus IS NULL OR v.deliveryStatus = :deliveryStatus) " +
            "AND (:paymentStatus IS NULL OR v.paymentStatus = :paymentStatus) " +
            "AND (:paymentDate IS NULL OR DATE(v.paymentDate) = :paymentDate) " +
            "AND (:createdBy IS NULL OR v.createdBy LIKE %:createdBy%)")
    Page<Video> getAll(
            Pageable pageable,
            VideoStatus videoStatus,
            @Param("assignedStaff") String assignedStaff,
            @Param("deliveryStatus") DeliveryStatus deliveryStatus,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("paymentDate") LocalDate paymentDate,
            @Param("createdBy") String createdBy
    );

    @Query("SELECT DISTINCT v.assignedStaff FROM Video v WHERE v.assignedStaff IS NOT NULL AND v.isDeleted = false")
    List<String> findDistinctAssignedStaff();

    @Query("SELECT DISTINCT v.createdBy FROM Video v WHERE v.createdBy IS NOT NULL AND v.isDeleted = false")
    List<String> findDistinctCreatedBy();

    @Query("SELECT new com.ptit.google.veo3.dto.StaffSalaryDto(" +
            "COALESCE(v.assignedStaff, 'Chưa ai nhận'), " +
            "SUM(v.orderValue), " +
            "COUNT(v)) " +
            "FROM Video v " +
            "WHERE v.isDeleted = false " +
            "AND v.paymentStatus IN ('DA_THANH_TOAN', 'BUNG') " +
            "AND (:date IS NULL OR DATE(v.paymentDate) = :date) " +
            "GROUP BY v.assignedStaff")
    List<StaffSalaryDto> calculateStaffSalaries(@Param("date") LocalDate date);

    /**
     * Đếm số video theo nhân viên được giao và trạng thái
     */
    long countByAssignedStaffAndStatus(String assignedStaff, VideoStatus status);

    /**
     * Đếm số video theo nhân viên được giao và trạng thái giao hàng
     */
    long countByAssignedStaffAndDeliveryStatus(String assignedStaff, DeliveryStatus deliveryStatus);

    /**
     * Đếm tổng số video đang trong quá trình làm việc của một nhân viên
     * Bao gồm: DANG_LAM, DANG_SUA và video có deliveryStatus = CAN_SUA_GAP
     * 
     * Query này tính:
     * - Video có status = DANG_LAM hoặc DANG_SUA 
     * - Video có deliveryStatus = CAN_SUA_GAP (bất kể status)
     * - Loại trừ duplicate (video có thể vừa DANG_SUA vừa CAN_SUA_GAP)
     * 
     * @param assignedStaff Tên nhân viên cần kiểm tra
     * @return Tổng số video đang trong quá trình làm việc
     */
    @Query("""
            SELECT COUNT(DISTINCT v.id) 
            FROM Video v 
            WHERE v.isDeleted = false 
            AND v.assignedStaff = :assignedStaff 
            AND (
                v.status IN ('DANG_LAM', 'DANG_SUA') 
                OR v.deliveryStatus = 'CAN_SUA_GAP'
            )
            """)
    long countActiveWorkloadByAssignedStaff(@Param("assignedStaff") String assignedStaff);

    /**
     * Lấy chi tiết các video đang trong quá trình làm việc của một nhân viên
     * Sử dụng để debugging và logging chi tiết
     * 
     * @param assignedStaff Tên nhân viên cần kiểm tra  
     * @return Danh sách video đang active
     */
    @Query("""
            SELECT v 
            FROM Video v 
            WHERE v.isDeleted = false 
            AND v.assignedStaff = :assignedStaff 
            AND (
                v.status IN ('DANG_LAM', 'DANG_SUA') 
                OR v.deliveryStatus = 'CAN_SUA_GAP'
            )
            ORDER BY v.createdAt DESC
            """)
    List<Video> findActiveWorkloadByAssignedStaff(@Param("assignedStaff") String assignedStaff);

    /**
     * Tìm các video đã quá thời hạn và cần được auto-reset
     * Video được coi là quá hạn nếu:
     * 1. Đã được assign (assignedAt không null, assignedStaff không null)
     * 2. Đang trong trạng thái DANG_LAM hoặc DANG_SUA
     * 3. Thời gian assign đã quá số phút được chỉ định
     * 
     * @return Danh sách video cần được reset
     */
    @Query("""
            SELECT v 
            FROM Video v 
            WHERE v.isDeleted = false 
            AND v.assignedAt IS NOT NULL 
            AND v.assignedStaff IS NOT NULL 
            AND v.assignedStaff != ''
            AND v.status IN ('DANG_LAM', 'DANG_SUA') 
            AND v.assignedAt < :expiredTime
            ORDER BY v.assignedAt ASC
            """)
    List<Video> findExpiredAssignedVideos(@Param("expiredTime") LocalDateTime expiredTime);

    /**
     * Đếm số lượng video đã quá hạn - dùng cho monitoring
     * 
     * @param expiredTime Thời điểm bắt đầu tính là quá hạn
     * @return Số lượng video quá hạn
     */
    @Query("""
            SELECT COUNT(v) 
            FROM Video v 
            WHERE v.isDeleted = false 
            AND v.assignedAt IS NOT NULL 
            AND v.assignedStaff IS NOT NULL 
            AND v.assignedStaff != ''
            AND v.status IN ('DANG_LAM', 'DANG_SUA') 
            AND v.assignedAt < :expiredTime
            """)
    long countExpiredAssignedVideos(@Param("expiredTime") LocalDateTime expiredTime);

    /**
     * Tính lương sales theo từng ngày - Sử dụng Interface Projection
     * 
     * Business Logic:
     * - GROUP BY createdBy (sales person)
     * - WHERE paymentStatus = 'DA_THANH_TOAN' 
     * - WHERE DATE(paymentDate) = targetDate
     * - SUM(price) per sales
     * - Commission = totalPrice * 0.12
     * 
     * Sử dụng interface projection thay cho constructor expression
     * để tránh lỗi "Missing constructor" trong JPQL
     * 
     * @param targetDate Ngày cần thống kê lương (yyyy-MM-dd)
     * @return Danh sách projection chứa thông tin lương sales theo ngày
     */
    @Query("""
            SELECT 
                COALESCE(v.createdBy, 'Unknown Sales') as salesName,
                COUNT(v) as totalPaidVideos,
                COALESCE(SUM(v.price), 0) as totalSalesValue,
                COALESCE(SUM(v.price) * 0.12, 0) as commissionSalary
            FROM Video v 
            WHERE v.isDeleted = false 
            AND v.paymentStatus = 'DA_THANH_TOAN' 
            AND DATE(v.paymentDate) = :targetDate 
            AND v.price IS NOT NULL
            GROUP BY v.createdBy
            ORDER BY SUM(v.price) DESC
            """)
    List<SalesSalaryProjection> calculateSalesSalariesProjectionByDate(@Param("targetDate") LocalDate targetDate);

    /**
     * BACKUP: Native query để tính lương sales - sử dụng nếu JPQL projection có vấn đề
     * 
     * @param targetDate Ngày cần thống kê lương (yyyy-MM-dd)
     * @return Danh sách Object[] chứa: salesName, totalVideos, totalSalesValue, commissionSalary
     */
    @Query(value = """
            SELECT 
                COALESCE(v.created_by, 'Unknown Sales') as sales_name,
                COUNT(v.id) as total_paid_videos,
                COALESCE(SUM(v.price), 0) as total_sales_value,
                COALESCE(SUM(v.price) * 0.12, 0) as commission_salary
            FROM videos v 
            WHERE v.is_deleted = false 
            AND v.payment_status = 'DA_THANH_TOAN' 
            AND DATE(v.payment_date) = :targetDate 
            AND v.price IS NOT NULL
            GROUP BY v.created_by
            ORDER BY SUM(v.price) DESC
            """, nativeQuery = true)
    List<Object[]> calculateSalesSalariesNativeByDate(@Param("targetDate") LocalDate targetDate);

}