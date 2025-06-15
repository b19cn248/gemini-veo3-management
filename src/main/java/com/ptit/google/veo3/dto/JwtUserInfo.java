package com.ptit.google.veo3.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO class chứa thông tin người dùng từ JWT token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtUserInfo {
    private String name;
    private String email;
    private String preferredUsername;
    private String subject;
}