package com.ptit.google.veo3.dto;

import com.ptit.google.veo3.entity.DeliveryStatus;
import com.ptit.google.veo3.entity.PaymentStatus;
import com.ptit.google.veo3.entity.VideoStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO để trả về dữ liệu video cho client
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoResponseDto {
    private Long id;
    private String customerName;
    private String videoContent;
    private String imageUrl;
    private String videoDuration;
    private LocalDateTime deliveryTime;
    private String assignedStaff;
    private VideoStatus status;
    private String videoUrl;
    private LocalDateTime completedTime;
    private Boolean customerApproved;
    private String customerNote;
    private Boolean checked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private DeliveryStatus deliveryStatus;
    private PaymentStatus paymentStatus;
    private LocalDateTime paymentDate;
    private BigDecimal orderValue;
}
