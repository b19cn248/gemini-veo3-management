package com.ptit.google.veo3.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity đại diện cho bảng videos trong database
 * Chứa thông tin về quy trình làm video cho khách hàng
 * <p>
 * UPDATED: Kế thừa từ BaseEntity để có audit fields
 * Các trường createdAt, updatedAt đã được chuyển sang BaseEntity
 *
 * @author Generated
 * @since 1.0
 */
@Entity
@Table(name = "videos")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video extends BaseEntity {

    /**
     * Primary key - ID tự tăng
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tên khách hàng - trường bắt buộc
     */
    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    /**
     * Nội dung video - mô tả chi tiết
     */
    @Column(name = "video_content", columnDefinition = "TEXT")
    private String videoContent;

    /**
     * URL hình ảnh minh họa
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Thời lượng video tính theo giây
     */
    @Column(name = "video_duration")
    private Integer videoDuration;

    /**
     * Thời gian giao hàng dự kiến
     */
    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;

    /**
     * Thời gian được assign cho nhân viên
     * Dùng để tracking và auto-reset video sau 15 phút không hoàn thành
     */
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    /**
     * Nhân viên được giao nhiệm vụ
     */
    @Column(name = "assigned_staff", length = 255)
    private String assignedStaff;

    /**
     * Trạng thái video hiện tại
     * Mặc định là CHUA_AI_NHAN
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private VideoStatus status = VideoStatus.CHUA_AI_NHAN;

    /**
     * URL video sau khi hoàn thành
     */
    @Column(name = "video_url", length = 500)
    private String videoUrl;

    /**
     * Thời gian hoàn thành video
     */
    @Column(name = "completed_time")
    private LocalDateTime completedTime;

    /**
     * Khách hàng đã approve chưa
     */
    @Column(name = "customer_approved")
    @Builder.Default
    private Boolean customerApproved = false;

    /**
     * Ghi chú từ khách hàng
     */
    @Column(name = "customer_note", columnDefinition = "TEXT")
    private String customerNote;

    /**
     * Đã kiểm tra chưa
     */
    @Column(name = "checked")
    @Builder.Default
    private Boolean checked = false;

    /**
     * Trạng thái giao hàng
     * Mặc định là CHUA_GUI
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status")
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.CHUA_GUI;

    /**
     * Trạng thái thanh toán
     * Mặc định là CHUA_THANH_TOAN
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.CHUA_THANH_TOAN;

    /**
     * Ngày thanh toán thực tế
     */
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    /**
     * Giá trị đơn hàng
     */
    @Column(name = "order_value", precision = 15, scale = 2)
    private BigDecimal orderValue;

    /**
     * Giá bán video cho khách hàng
     * Được tính dựa trên order_value theo business rules
     */
    @Column(name = "price", precision = 15, scale = 2)
    private BigDecimal price;

    /**
     * URL hình ảnh hóa đơn thanh toán
     * Chỉ hiển thị cho admin hoặc người tạo đơn
     */
    @Column(name = "bill_image_url", length = 500)
    private String billImageUrl;

    /**
     * Link Facebook của khách hàng
     * Thông tin liên hệ bổ sung
     */
    @Column(name = "linkfb", length = 500)
    private String linkfb;

    /**
     * Số điện thoại của khách hàng
     * Thông tin liên hệ bổ sung
     */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /**
     * Quan hệ Many-to-One với User (người được giao)
     * OPTIONAL: Có thể null nếu chưa assign cho ai
     * Sử dụng LAZY loading để tối ưu performance
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id", foreignKey = @ForeignKey(name = "fk_video_assigned_user"))
    private User assignedUser;


    /**
     * Business method: Assign video cho user
     *
     * @param user User được giao việc
     */
    public void assignToUser(User user) {
        this.assignedUser = user;
        this.assignedStaff = user != null ? user.getDisplayName() : null;
        this.assignedAt = user != null ? LocalDateTime.now() : null;
        if (this.status == VideoStatus.CHUA_AI_NHAN) {
            this.status = VideoStatus.DANG_LAM;
        }
    }

    /**
     * Business method: Unassign video
     */
    public void unassign() {
        this.assignedUser = null;
        this.assignedStaff = null;
        this.assignedAt = null;
        this.status = VideoStatus.CHUA_AI_NHAN;
    }

    /**
     * Business method: Auto-reset video khi quá thời hạn làm việc
     * Được gọi bởi scheduled job
     */
    public void autoReset() {
        this.assignedUser = null;
        this.assignedStaff = null;
        this.assignedAt = null;
        this.status = VideoStatus.CHUA_AI_NHAN;
    }

    /**
     * Business method: Kiểm tra video có quá hạn làm việc không
     * @param timeoutMinutes Số phút timeout (mặc định 15)
     * @return true nếu video đã quá hạn
     */
    public boolean isAssignmentExpired(int timeoutMinutes) {
        if (this.assignedAt == null || this.assignedStaff == null || this.assignedStaff.trim().isEmpty()) {
            return false;
        }
        
        // Chỉ check timeout cho video đang trong trạng thái làm việc
        if (this.status != VideoStatus.DANG_LAM && this.status != VideoStatus.DANG_SUA) {
            return false;
        }
        
        LocalDateTime expiredTime = this.assignedAt.plusMinutes(timeoutMinutes);
        return LocalDateTime.now().isAfter(expiredTime);
    }

    /**
     * Business method: Đánh dấu video đã hoàn thành
     */
    public void markAsCompleted() {
        this.status = VideoStatus.DA_XONG;
        this.completedTime = LocalDateTime.now();
    }

    /**
     * Business method: Khách hàng approve video
     */
    public void approveByCustomer() {
        this.customerApproved = true;
        this.deliveryStatus = DeliveryStatus.DA_GUI;
    }

    /**
     * Business method: Kiểm tra video có thể bị xóa không
     *
     * @return true nếu có thể xóa (chưa thanh toán hoặc chưa giao)
     */
    public boolean canBeDeleted() {
        return paymentStatus != PaymentStatus.DA_THANH_TOAN ||
                deliveryStatus != DeliveryStatus.DA_GUI;
    }
}