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
     * Check if current user has specific role in resource_access
     * @param clientId Client ID in resource_access (e.g., "video-veo3-be")
     * @param role Role to check (e.g., "admin")
     * @return true if user has the role, false otherwise
     */
    boolean hasResourceRole(String clientId, String role);
    
    /**
     * Check if current user has admin role in video-veo3-be client
     * @return true if user is admin in video-veo3-be, false otherwise
     */
    boolean isVideoVeo3BeAdmin();
    
    /**
     * Check if current user has permission to update video
     * @param assignedStaff Staff assigned to the video
     * @return true if user has permission, false otherwise
     */
    boolean hasPermissionToUpdateVideo(String assignedStaff);
}