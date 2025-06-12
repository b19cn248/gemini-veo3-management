package com.ptit.google.veo3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Service class để xử lý JWT token và extract thông tin người dùng
 * 
 * Dịch vụ này chịu trách nhiệm:
 * - Lấy JWT token từ Security Context
 * - Extract các thông tin từ JWT claims như tên người dùng
 * - Validate và xử lý các trường hợp exception
 * 
 * @author Generated
 * @since 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JwtTokenService {

    /**
     * Lấy tên người dùng từ JWT token hiện tại trong Security Context
     * 
     * Phương thức này:
     * 1. Lấy Authentication từ SecurityContextHolder
     * 2. Kiểm tra xem đó có phải JwtAuthenticationToken không
     * 3. Extract claim "name" từ JWT token
     * 4. Trả về tên người dùng hoặc null nếu không tìm thấy
     * 
     * @return Tên người dùng từ JWT claim "name", hoặc null nếu không có
     * @throws IllegalStateException nếu không có authentication context hoặc token không hợp lệ
     */
    public String getCurrentUserNameFromJwt() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null) {
                log.warn("No authentication found in SecurityContext");
                throw new IllegalStateException("Không tìm thấy thông tin xác thực người dùng");
            }
            
            if (!(authentication instanceof JwtAuthenticationToken jwtAuthToken)) {
                log.warn("Authentication is not a JwtAuthenticationToken, found: {}", 
                        authentication.getClass().getSimpleName());
                throw new IllegalStateException("Token xác thực không hợp lệ");
            }
            
            Jwt jwt = jwtAuthToken.getToken();
            if (jwt == null) {
                log.warn("JWT token is null in authentication");
                throw new IllegalStateException("JWT token không hợp lệ");
            }
            
            // Extract tên người dùng từ claim "name"
            String userName = jwt.getClaimAsString("name");
            
            if (userName == null || userName.trim().isEmpty()) {
                log.warn("JWT token does not contain 'name' claim or it's empty");
                throw new IllegalStateException("JWT token không chứa thông tin tên người dùng");
            }
            
            log.debug("Successfully extracted user name from JWT: {}", userName);
            return userName.trim();
            
        } catch (Exception e) {
            log.error("Error extracting user name from JWT token: ", e);
            if (e instanceof IllegalStateException) {
                throw e;
            }
            throw new IllegalStateException("Lỗi khi lấy thông tin người dùng từ token: " + e.getMessage(), e);
        }
    }
    
    /**
     * Lấy thông tin người dùng chi tiết từ JWT token
     * 
     * @return JwtUserInfo chứa các thông tin cơ bản của người dùng
     */
    public JwtUserInfo getCurrentUserInfoFromJwt() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null) {
                log.warn("No authentication found in SecurityContext");
                throw new IllegalStateException("Không tìm thấy thông tin xác thực người dùng");
            }
            
            if (!(authentication instanceof JwtAuthenticationToken jwtAuthToken)) {
                log.warn("Authentication is not a JwtAuthenticationToken, found: {}", 
                        authentication.getClass().getSimpleName());
                throw new IllegalStateException("Token xác thực không hợp lệ");
            }
            
            Jwt jwt = jwtAuthToken.getToken();
            if (jwt == null) {
                log.warn("JWT token is null in authentication");
                throw new IllegalStateException("JWT token không hợp lệ");
            }
            
            // Extract các claims cần thiết
            String name = jwt.getClaimAsString("name");
            String email = jwt.getClaimAsString("email");
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            String subject = jwt.getClaimAsString("sub");
            
            log.debug("Successfully extracted user info from JWT - name: {}, email: {}, username: {}", 
                    name, email, preferredUsername);
                    
            return JwtUserInfo.builder()
                    .name(name)
                    .email(email)
                    .preferredUsername(preferredUsername)
                    .subject(subject)
                    .build();
            
        } catch (Exception e) {
            log.error("Error extracting user info from JWT token: ", e);
            if (e instanceof IllegalStateException) {
                throw e;
            }
            throw new IllegalStateException("Lỗi khi lấy thông tin người dùng từ token: " + e.getMessage(), e);
        }
    }
    
    /**
     * Kiểm tra xem người dùng hiện tại có quyền cập nhật video không
     * 
     * @param assignedStaff Tên nhân viên được giao trong video
     * @return true nếu người dùng có quyền cập nhật, false nếu không
     */
    public boolean hasPermissionToUpdateVideo(String assignedStaff) {
        try {
            String currentUserName = getCurrentUserNameFromJwt();
            
            // Kiểm tra xem assignedStaff có null hoặc empty không
            if (assignedStaff == null || assignedStaff.trim().isEmpty()) {
                log.debug("Video chưa được giao cho ai, không cho phép cập nhật");
                return false;
            }
            
            // So sánh tên người dùng hiện tại với assignedStaff (không phân biệt hoa thường)
            boolean hasPermission = currentUserName.equalsIgnoreCase(assignedStaff.trim());
            
            log.debug("Permission check - Current user: '{}', Assigned staff: '{}', Has permission: {}", 
                    currentUserName, assignedStaff, hasPermission);
                    
            return hasPermission;
            
        } catch (Exception e) {
            log.error("Error checking video update permission: ", e);
            // Trả về false để an toàn khi có lỗi
            return false;
        }
    }
    
    /**
     * DTO class chứa thông tin người dùng từ JWT
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class JwtUserInfo {
        private String name;
        private String email;
        private String preferredUsername;
        private String subject;
    }
}