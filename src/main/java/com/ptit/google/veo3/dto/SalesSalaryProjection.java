package com.ptit.google.veo3.dto;

import java.math.BigDecimal;

/**
 * Interface projection cho Sales Salary data
 * Sử dụng thay cho constructor expression để tránh lỗi JPQL
 * 
 * Spring Data JPA sẽ tự động implement interface này
 * và map kết quả query vào các methods
 */
public interface SalesSalaryProjection {
    
    /**
     * Tên sales person (từ createdBy)
     */
    String getSalesName();
    
    /**
     * Tổng số video đã thanh toán
     */
    Long getTotalPaidVideos();
    
    /**
     * Tổng giá trị doanh số
     */
    BigDecimal getTotalSalesValue();
    
    /**
     * Tổng hoa hồng (được tính trong query)
     */
    BigDecimal getCommissionSalary();
}