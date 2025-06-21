package com.ptit.google.veo3.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO để trả về thông tin liên hệ khách hàng
 * Chỉ chứa các thông tin cần thiết: tên, Facebook, số điện thoại
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerContactDto {
    
    /**
     * ID của video để có thể reference back nếu cần
     */
    private Long videoId;
    
    /**
     * Tên khách hàng
     */
    private String customerName;
    
    /**
     * Link Facebook của khách hàng
     * Frontend có thể render thành clickable link với target="_blank"
     */
    private String linkfb;
    
    /**
     * Số điện thoại của khách hàng
     * Frontend có thể render với tel: link hoặc copy button
     */
    private String phoneNumber;
}