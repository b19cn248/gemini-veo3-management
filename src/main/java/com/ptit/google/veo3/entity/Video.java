package com.ptit.google.veo3.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên khách hàng không được để trống")
    @Size(max = 100, message = "Tên khách hàng không được vượt quá 100 ký tự")
    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @NotBlank(message = "Nội dung video không được để trống")
    @Size(max = 500, message = "Nội dung video không được vượt quá 500 ký tự")
    @Column(name = "video_content", nullable = false, length = 500)
    private String videoContent;

    @Size(max = 255, message = "URL hình ảnh không được vượt quá 255 ký tự")
    @Column(name = "image_url")
    private String imageUrl;

    @Pattern(regexp = "^\\d{2}:\\d{2}:\\d{2}$", message = "Thời lượng video phải theo định dạng HH:mm:ss")
    @Column(name = "video_duration", length = 8)
    private String videoDuration;

    @Future(message = "Thời gian giao hàng phải trong tương lai")
    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;

    // Quan hệ Many-to-One với User
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_user_id", referencedColumnName = "id")
    private User assignedUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private VideoStatus status = VideoStatus.CHUA_AI_NHAN;

    @Size(max = 255, message = "URL video không được vượt quá 255 ký tự")
    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "completed_time")
    private LocalDateTime completedTime;

    @Column(name = "customer_approved")
    @Builder.Default
    private Boolean customerApproved = false;

    @Size(max = 500, message = "Ghi chú khách hàng không được vượt quá 500 ký tự")
    @Column(name = "customer_note", length = 500)
    private String customerNote;

    @Column(name = "checked")
    @Builder.Default
    private Boolean checked = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status")
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.CHUA_GUI;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.CHUA_THANH_TOAN;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá trị đơn hàng phải lớn hơn 0")
    @Digits(integer = 10, fraction = 2, message = "Giá trị đơn hàng không hợp lệ")
    @Column(name = "order_value", precision = 12, scale = 2)
    private BigDecimal orderValue;

    // Business logic methods
    @PreUpdate
    @PrePersist
    public void validatePaymentDate() {
        if (paymentStatus == PaymentStatus.DA_THANH_TOAN && paymentDate == null) {
            paymentDate = LocalDateTime.now();
        } else if (paymentStatus != PaymentStatus.DA_THANH_TOAN) {
            paymentDate = null;
        }
    }

    // Helper method để lấy tên nhân viên được giao
    public String getAssignedStaffName() {
        return assignedUser != null ? assignedUser.getFullName() : null;
    }
}
