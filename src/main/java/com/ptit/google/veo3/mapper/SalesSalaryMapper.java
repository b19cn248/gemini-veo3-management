package com.ptit.google.veo3.mapper;

import com.ptit.google.veo3.dto.SalesSalaryDto;
import com.ptit.google.veo3.dto.SalesSalaryProjection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SalesSalaryMapper {

    private static final BigDecimal THUONG_NGUYEN_COMMISSION_RATE = new BigDecimal("0.12"); // 12%
    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.10"); // 10%
    
    public SalesSalaryDto toDto(SalesSalaryProjection projection) {
        if (projection == null) {
            return null;
        }
        
        SalesSalaryDto dto = new SalesSalaryDto();
        
        dto.setSalesName(projection.getSalesName());
        dto.setTotalPaidVideos(projection.getTotalPaidVideos());
        dto.setTotalSalesValue(projection.getTotalSalesValue());
        dto.setCommissionSalary(projection.getCommissionSalary());
        
        BigDecimal commissionRate = calculateCommissionRate(projection.getSalesName());
        dto.setCommissionRate(commissionRate);
        
        return dto;
    }
    
    public SalesSalaryDto buildSalesSalaryDto(String salesName,
                                             String salaryDate,
                                             Long totalPaidVideos,
                                             BigDecimal totalSalesValue) {
        if (salesName == null) {
            return null;
        }
        
        SalesSalaryDto dto = new SalesSalaryDto();
        
        dto.setSalesName(salesName);
        dto.setSalaryDate(salaryDate);
        dto.setTotalPaidVideos(totalPaidVideos != null ? totalPaidVideos : 0L);
        dto.setTotalSalesValue(totalSalesValue != null ? totalSalesValue : BigDecimal.ZERO);
        
        BigDecimal commissionRate = calculateCommissionRate(salesName);
        dto.setCommissionRate(commissionRate);
        
        BigDecimal commissionSalary = calculateCommissionSalary(dto.getTotalSalesValue(), commissionRate);
        dto.setCommissionSalary(commissionSalary);
        
        return dto;
    }
    
    public SalesSalaryDto buildSalesSalaryDto(String salesName,
                                             LocalDate salaryDate,
                                             Long totalPaidVideos,
                                             BigDecimal totalSalesValue) {
        String salaryDateStr = salaryDate != null ? salaryDate.toString() : null;
        return buildSalesSalaryDto(salesName, salaryDateStr, totalPaidVideos, totalSalesValue);
    }
    
    public List<SalesSalaryDto> toDtoList(List<SalesSalaryProjection> projections) {
        if (projections == null) {
            return null;
        }
        
        return projections.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    private BigDecimal calculateCommissionRate(String salesName) {
        if (salesName == null) {
            return DEFAULT_COMMISSION_RATE;
        }
        
        String lowerSalesName = salesName.toLowerCase();
        if (lowerSalesName.contains("thuong nguyen") || lowerSalesName.contains("thuong")) {
            return THUONG_NGUYEN_COMMISSION_RATE;
        }
        
        return DEFAULT_COMMISSION_RATE;
    }
    
    private BigDecimal calculateCommissionSalary(BigDecimal totalSalesValue, BigDecimal commissionRate) {
        if (totalSalesValue == null || commissionRate == null) {
            return BigDecimal.ZERO;
        }
        
        return totalSalesValue.multiply(commissionRate);
    }
}