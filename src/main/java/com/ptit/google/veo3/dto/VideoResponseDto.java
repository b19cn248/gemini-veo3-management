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
    private Integer videoDuration; // Thời lượng video tính theo giây
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
    private String createdBy;

    /**
     * Helper method để format thời lượng video thành định dạng HH:mm:ss
     * @return String thời lượng video theo format HH:mm:ss
     */
    public String getFormattedVideoDuration() {
        if (videoDuration == null || videoDuration <= 0) {
            return "00:00:00";
        }

        int hours = videoDuration / 3600;
        int minutes = (videoDuration % 3600) / 60;
        int seconds = videoDuration % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}