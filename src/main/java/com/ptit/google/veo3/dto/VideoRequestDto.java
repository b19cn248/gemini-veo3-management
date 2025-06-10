package com.ptit.google.veo3.dto;

import com.ptit.google.veo3.entity.DeliveryStatus;
import com.ptit.google.veo3.entity.PaymentStatus;
import com.ptit.google.veo3.entity.VideoStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO để nhận dữ liệu từ client khi tạo/cập nhật video
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoRequestDto {

    @NotBlank(message = "Tên khách hàng không được để trống")
    @Size(max = 255, message = "Tên khách hàng không được vượt quá 255 ký tự")
    private String customerName;

    @Size(max = 5000, message = "Nội dung video không được vượt quá 5000 ký tự")
    private String videoContent;

    @Size(max = 500, message = "URL hình ảnh không được vượt quá 500 ký tự")
    private String imageUrl;

    @Min(value = 1, message = "Thời lượng video phải lớn hơn 0 giây")
    @Max(value = 86400, message = "Thời lượng video không được vượt quá 24 giờ (86400 giây)")
    private Integer videoDuration; // Thời lượng video tính theo giây

    @Future(message = "Thời gian giao hàng phải là thời gian trong tương lai")
    private LocalDateTime deliveryTime;

    @Size(max = 255, message = "Tên nhân viên được giao không được vượt quá 255 ký tự")
    private String assignedStaff;

    private VideoStatus status;

    @Size(max = 500, message = "URL video không được vượt quá 500 ký tự")
    private String videoUrl;

    private LocalDateTime completedTime;

    private Boolean customerApproved;

    @Size(max = 5000, message = "Ghi chú khách hàng không được vượt quá 5000 ký tự")
    private String customerNote;

    private Boolean checked;

    private DeliveryStatus deliveryStatus;

    private PaymentStatus paymentStatus;

    private LocalDateTime paymentDate;

    private BigDecimal orderValue;
}