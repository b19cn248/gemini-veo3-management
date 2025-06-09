package com.ptit.google.veo3.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity đại diện cho bảng videos trong database
 * Chứa thông tin về quy trình làm video cho khách hàng
 */
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

    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    @Column(name = "video_content", columnDefinition = "TEXT")
    private String videoContent;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "video_duration")
    private Integer videoDuration; // Thời lượng video tính theo giây

    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;

    @Column(name = "assigned_staff", length = 255)
    private String assignedStaff;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private VideoStatus status = VideoStatus.CHUA_AI_NHAN;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "completed_time")
    private LocalDateTime completedTime;

    @Column(name = "customer_approved")
    @Builder.Default
    private Boolean customerApproved = false;

    @Column(name = "customer_note", columnDefinition = "TEXT")
    private String customerNote;

    @Column(name = "checked")
    @Builder.Default
    private Boolean checked = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
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

    @Column(name = "order_value", precision = 15, scale = 2)
    private BigDecimal orderValue;

    /**
     * Business logic: Tự động set paymentDate khi paymentStatus = DA_THANH_TOAN
     */
    @PrePersist
    @PreUpdate
    private void validatePaymentDate() {
        if (paymentStatus == PaymentStatus.DA_THANH_TOAN && paymentDate == null) {
            paymentDate = LocalDateTime.now();
        } else if (paymentStatus != PaymentStatus.DA_THANH_TOAN) {
            paymentDate = null;
        }
    }
}