package com.ptit.google.veo3.service.interfaces;

import com.ptit.google.veo3.dto.JwtUserInfo;

/**
 * Interface for JWT Token Service operations
 * Following Interface Segregation Principle (ISP) and Dependency Inversion Principle (DIP)
 */
public interface IJwtTokenService {
    
    /**
     * Get current user name from JWT token
     * @return User name from JWT claim, or null if not found
     */
    String getCurrentUserNameFromJwt();
    
    /**
     * Get complete user information from JWT token
     * @return JwtUserInfo object containing user details
     */
    JwtUserInfo getCurrentUserInfoFromJwt();
    
    /**
     * Check if current user has admin role
     * @return true if user is admin, false otherwise
     */
    boolean isCurrentUserAdmin();
    
    /**
     * Check if current user has permission to update video
     * @param assignedStaff Staff assigned to the video
     * @return true if user has permission, false otherwise
     */
    boolean hasPermissionToUpdateVideo(String assignedStaff);
}