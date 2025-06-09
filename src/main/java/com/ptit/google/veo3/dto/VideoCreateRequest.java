package com.ptit.google.veo3.dto;

import com.ptit.google.veo3.entity.DeliveryStatus;
import com.ptit.google.veo3.entity.PaymentStatus;
import com.ptit.google.veo3.entity.VideoStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VideoCreateRequest {

    @NotBlank(message = "Tên khách hàng không được để trống")
    @Size(max = 100, message = "Tên khách hàng không được vượt quá 100 ký tự")
    private String customerName;

    @NotBlank(message = "Nội dung video không được để trống")
    @Size(max = 500, message = "Nội dung video không được vượt quá 500 ký tự")
    private String videoContent;

    @Size(max = 255, message = "URL hình ảnh không được vượt quá 255 ký tự")
    private String imageUrl;

    @Pattern(regexp = "^\\d{2}:\\d{2}:\\d{2}$", message = "Thời lượng video phải theo định dạng HH:mm:ss")
    private String videoDuration;

    @Future(message = "Thời gian giao hàng phải trong tương lai")
    private LocalDateTime deliveryTime;

    private Long assignedUserId; // ID của user được giao

    private VideoStatus status = VideoStatus.CHUA_AI_NHAN;

    @Size(max = 255, message = "URL video không được vượt quá 255 ký tự")
    private String videoUrl;

    private LocalDateTime completedTime;

    private Boolean customerApproved = false;

    @Size(max = 500, message = "Ghi chú khách hàng không được vượt quá 500 ký tự")
    private String customerNote;

    private Boolean checked = false;

    private DeliveryStatus deliveryStatus = DeliveryStatus.CHUA_GUI;

    private PaymentStatus paymentStatus = PaymentStatus.CHUA_THANH_TOAN;

    private LocalDateTime paymentDate;

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá trị đơn hàng phải lớn hơn 0")
    @Digits(integer = 10, fraction = 2, message = "Giá trị đơn hàng không hợp lệ")
    private BigDecimal orderValue;
}