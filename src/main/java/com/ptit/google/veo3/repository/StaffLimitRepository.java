package com.ptit.google.veo3.repository;

import com.ptit.google.veo3.entity.StaffLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface để thao tác với bảng staff_limits
 */
@Repository
public interface StaffLimitRepository extends JpaRepository<StaffLimit, Long> {

    /**
     * Tìm limit đang active của một nhân viên
     * 
     * @param staffName Tên nhân viên
     * @return Optional chứa StaffLimit nếu tìm thấy
     */
    Optional<StaffLimit> findByStaffNameAndIsActiveTrue(String staffName);

    /**
     * Check xem nhân viên có bị limit không tại thời điểm hiện tại
     * 
     * @param staffName Tên nhân viên
     * @param currentTime Thời điểm hiện tại
     * @return true nếu nhân viên đang bị limit
     */
    boolean existsByStaffNameAndIsActiveTrueAndEndDateAfter(String staffName, LocalDateTime currentTime);

    /**
     * Tìm limit đang active của một nhân viên tại thời điểm hiện tại
     * 
     * @param staffName Tên nhân viên
     * @param currentTime Thời điểm hiện tại
     * @return Optional chứa StaffLimit nếu tìm thấy
     */
    Optional<StaffLimit> findByStaffNameAndIsActiveTrueAndEndDateAfter(String staffName, LocalDateTime currentTime);

    /**
     * Lấy danh sách tất cả limits đang active
     * 
     * @return List chứa các StaffLimit đang active
     */
    List<StaffLimit> findByIsActiveTrueOrderByCreatedAtDesc();

    /**
     * Lấy danh sách limits đang active và chưa hết hạn
     * 
     * @param currentTime Thời điểm hiện tại
     * @return List chứa các StaffLimit đang có hiệu lực
     */
    @Query("SELECT sl FROM StaffLimit sl WHERE sl.isActive = true AND sl.endDate > :currentTime ORDER BY sl.endDate ASC")
    List<StaffLimit> findActiveLimitsNotExpired(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Deactivate tất cả limits của một nhân viên
     * Sử dụng khi tạo limit mới hoặc khi admin hủy limit
     * 
     * @param staffName Tên nhân viên
     * @return Số lượng records đã được update
     */
    @Modifying
    @Transactional
    @Query("UPDATE StaffLimit sl SET sl.isActive = false WHERE sl.staffName = :staffName AND sl.isActive = true")
    int deactivateAllLimitsByStaffName(@Param("staffName") String staffName);

    /**
     * Tự động deactivate các limits đã hết hạn
     * Có thể sử dụng trong scheduled job để cleanup
     * 
     * @param currentTime Thời điểm hiện tại
     * @return Số lượng limits đã được deactivate
     */
    @Modifying
    @Query("UPDATE StaffLimit sl SET sl.isActive = false WHERE sl.isActive = true AND sl.endDate <= :currentTime")
    int deactivateExpiredLimits(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Lấy tất cả limits của một nhân viên (bao gồm cả inactive)
     * Hữu ích cho audit và history tracking
     * 
     * @param staffName Tên nhân viên
     * @return List chứa tất cả limits của nhân viên
     */
    List<StaffLimit> findByStaffNameOrderByCreatedAtDesc(String staffName);

    /**
     * Đếm số lượng limits đang active trong hệ thống
     * 
     * @return Số lượng limits đang active
     */
    long countByIsActiveTrue();

    /**
     * Tìm kiếm limits theo khoảng thời gian
     * 
     * @param startTime Thời điểm bắt đầu
     * @param endTime Thời điểm kết thúc
     * @return List chứa các limits trong khoảng thời gian
     */
    @Query("SELECT sl FROM StaffLimit sl WHERE sl.createdAt BETWEEN :startTime AND :endTime ORDER BY sl.createdAt DESC")
    List<StaffLimit> findLimitsByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                                  @Param("endTime") LocalDateTime endTime);
}