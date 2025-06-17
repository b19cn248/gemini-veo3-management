package com.ptit.google.veo3.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO để trả về thông tin lương sales theo từng ngày
 * 
 * Tính lương dựa trên:
 * - Tổng giá trị price của videos đã thanh toán trong ngày
 * - Hoa hồng biến động: 12% cho 'Thuong Nguyen'/'thuong', 10% cho những sales khác
 * - Group theo createdBy (sales person)
 * - Filter theo paymentDate
 * 
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesSalaryDto {
    
    /**
     * Tên sales (lấy từ createdBy)
     */
    private String salesName;
    
    /**
     * Ngày thống kê lương (yyyy-MM-dd format)
     */
    private String salaryDate;
    
    /**
     * Tổng số video đã thanh toán trong ngày
     */
    private Long totalPaidVideos;
    
    /**
     * Tổng giá trị price của videos đã thanh toán
     */
    private BigDecimal totalSalesValue;
    
    /**
     * Tiền lương = totalSalesValue * commissionRate
     * CommissionRate: 12% cho 'Thuong Nguyen'/'thuong', 10% cho sales khác
     */
    private BigDecimal commissionSalary;
    
    /**
     * Tỷ lệ hoa hồng (12% cho 'Thuong Nguyen'/'thuong', 10% cho sales khác)
     */
    private BigDecimal commissionRate;
}