package com.ptit.google.veo3.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity để lưu thông tin giới hạn số đơn nhân viên có thể nhận
 * 
 * Business Logic:
 * - Mỗi nhân viên chỉ có thể có một limit active tại một thời điểm
 * - Khi tạo limit mới, limit cũ sẽ được deactivate
 * - Limit sẽ tự động expire khi end_date qua
 * 
 * Security:
 * - Chỉ có ADMIN mới có quyền tạo/xóa limit
 * - Hệ thống sẽ check limit khi assign video cho nhân viên
 */
@Entity
@Table(name = "staff_limits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Người tạo record này
     */
    @Column(name = "created_by", length = 255)
    private String createdBy;

    /**
     * Thời điểm tạo record
     */
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Tên nhân viên bị giới hạn
     * Phải match với assignedStaff trong bảng videos
     */
    @Column(name = "staff_name", nullable = false, length = 255)
    private String staffName;

    /**
     * Thời điểm bắt đầu áp dụng giới hạn
     */
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    /**
     * Thời điểm kết thúc giới hạn
     * Sau thời điểm này, nhân viên có thể nhận đơn bình thường
     */
    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    /**
     * Trạng thái active của limit
     * - true: Limit đang có hiệu lực
     * - false: Limit đã bị hủy hoặc hết hạn
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Check xem limit có còn hiệu lực không
     * 
     * @return true nếu limit đang active và chưa hết hạn
     */
    public boolean isCurrentlyActive() {
        return isActive && endDate.isAfter(LocalDateTime.now());
    }

    /**
     * Tính số ngày còn lại của limit
     * 
     * @return số ngày còn lại, 0 nếu đã hết hạn
     */
    public long getRemainingDays() {
        if (!isCurrentlyActive()) {
            return 0;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (endDate.isAfter(now)) {
            return java.time.Duration.between(now, endDate).toDays();
        }
        
        return 0;
    }

    /**
     * Deactivate limit
     * Sử dụng khi admin muốn hủy limit sớm
     */
    public void deactivate() {
        this.isActive = false;
    }
}