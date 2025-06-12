package com.ptit.google.veo3.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho JwtTokenService
 * 
 * Test cases:
 * 1. Successful extraction of user name from valid JWT
 * 2. Exception when no authentication in SecurityContext
 * 3. Exception when authentication is not JwtAuthenticationToken
 * 4. Exception when JWT token is null
 * 5. Exception when 'name' claim is missing or empty
 * 6. Permission check scenarios
 * 
 * @author Generated
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private Authentication authentication;
    
    @Mock
    private JwtAuthenticationToken jwtAuthenticationToken;
    
    @Mock
    private Jwt jwt;
    
    private JwtTokenService jwtTokenService;
    
    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService();
        SecurityContextHolder.setContext(securityContext);
    }
    
    @Test
    void getCurrentUserNameFromJwt_Success() {
        // Arrange
        String expectedUserName = "Nguyễn Minh Hiếu";
        
        when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);
        when(jwtAuthenticationToken.getToken()).thenReturn(jwt);
        when(jwt.getClaimAsString("name")).thenReturn(expectedUserName);
        
        // Act
        String actualUserName = jwtTokenService.getCurrentUserNameFromJwt();
        
        // Assert
        assertEquals(expectedUserName, actualUserName);
        verify(jwt).getClaimAsString("name");
    }
    
    @Test
    void getCurrentUserNameFromJwt_NoAuthentication_ThrowsException() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);
        
        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> jwtTokenService.getCurrentUserNameFromJwt()
        );
        
        assertEquals("Không tìm thấy thông tin xác thực người dùng", exception.getMessage());
    }
    
    @Test
    void getCurrentUserNameFromJwt_NotJwtAuthenticationToken_ThrowsException() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        
        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> jwtTokenService.getCurrentUserNameFromJwt()
        );
        
        assertEquals("Token xác thực không hợp lệ", exception.getMessage());
    }
    
    @Test
    void getCurrentUserNameFromJwt_NullJwtToken_ThrowsException() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);
        when(jwtAuthenticationToken.getToken()).thenReturn(null);
        
        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> jwtTokenService.getCurrentUserNameFromJwt()
        );
        
        assertEquals("JWT token không hợp lệ", exception.getMessage());
    }
    
    @Test
    void getCurrentUserNameFromJwt_EmptyNameClaim_ThrowsException() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);
        when(jwtAuthenticationToken.getToken()).thenReturn(jwt);
        when(jwt.getClaimAsString("name")).thenReturn("");
        
        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> jwtTokenService.getCurrentUserNameFromJwt()
        );
        
        assertEquals("JWT token không chứa thông tin tên người dùng", exception.getMessage());
    }
    
    @Test
    void hasPermissionToUpdateVideo_SameUser_ReturnsTrue() {
        // Arrange
        String userName = "Nguyễn Minh Hiếu";
        String assignedStaff = "Nguyễn Minh Hiếu";
        
        when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);
        when(jwtAuthenticationToken.getToken()).thenReturn(jwt);
        when(jwt.getClaimAsString("name")).thenReturn(userName);
        
        // Act
        boolean hasPermission = jwtTokenService.hasPermissionToUpdateVideo(assignedStaff);
        
        // Assert
        assertTrue(hasPermission);
    }
    
    @Test
    void hasPermissionToUpdateVideo_DifferentUser_ReturnsFalse() {
        // Arrange
        String userName = "Nguyễn Minh Hiếu";
        String assignedStaff = "Trần Văn Nam";
        
        when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);
        when(jwtAuthenticationToken.getToken()).thenReturn(jwt);
        when(jwt.getClaimAsString("name")).thenReturn(userName);
        
        // Act
        boolean hasPermission = jwtTokenService.hasPermissionToUpdateVideo(assignedStaff);
        
        // Assert
        assertFalse(hasPermission);
    }
    
    @Test
    void hasPermissionToUpdateVideo_CaseInsensitive_ReturnsTrue() {
        // Arrange
        String userName = "Nguyễn Minh Hiếu";
        String assignedStaff = "NGUYỄN MINH HIẾU";
        
        when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);
        when(jwtAuthenticationToken.getToken()).thenReturn(jwt);
        when(jwt.getClaimAsString("name")).thenReturn(userName);
        
        // Act
        boolean hasPermission = jwtTokenService.hasPermissionToUpdateVideo(assignedStaff);
        
        // Assert
        assertTrue(hasPermission);
    }
    
    @Test
    void hasPermissionToUpdateVideo_NullAssignedStaff_ReturnsFalse() {
        // Arrange
        String userName = "Nguyễn Minh Hiếu";
        
        when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);
        when(jwtAuthenticationToken.getToken()).thenReturn(jwt);
        when(jwt.getClaimAsString("name")).thenReturn(userName);
        
        // Act
        boolean hasPermission = jwtTokenService.hasPermissionToUpdateVideo(null);
        
        // Assert
        assertFalse(hasPermission);
    }
    
    @Test
    void hasPermissionToUpdateVideo_EmptyAssignedStaff_ReturnsFalse() {
        // Arrange
        String userName = "Nguyễn Minh Hiếu";
        
        when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);
        when(jwtAuthenticationToken.getToken()).thenReturn(jwt);
        when(jwt.getClaimAsString("name")).thenReturn(userName);
        
        // Act
        boolean hasPermission = jwtTokenService.hasPermissionToUpdateVideo("   ");
        
        // Assert
        assertFalse(hasPermission);
    }
    
    @Test
    void hasPermissionToUpdateVideo_ExceptionInGetCurrentUser_ReturnsFalse() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);
        
        // Act
        boolean hasPermission = jwtTokenService.hasPermissionToUpdateVideo("Nguyễn Minh Hiếu");
        
        // Assert
        assertFalse(hasPermission);
    }
    
    @Test
    void getCurrentUserInfoFromJwt_Success() {
        // Arrange
        String expectedName = "Nguyễn Minh Hiếu";
        String expectedEmail = "hieu1234.ptit@gmail.com";
        String expectedUsername = "admin";
        String expectedSubject = "c141449e-2e5c-4d20-916a-0678e2a62449";
        
        when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);
        when(jwtAuthenticationToken.getToken()).thenReturn(jwt);
        when(jwt.getClaimAsString("name")).thenReturn(expectedName);
        when(jwt.getClaimAsString("email")).thenReturn(expectedEmail);
        when(jwt.getClaimAsString("preferred_username")).thenReturn(expectedUsername);
        when(jwt.getClaimAsString("sub")).thenReturn(expectedSubject);
        
        // Act
        JwtTokenService.JwtUserInfo userInfo = jwtTokenService.getCurrentUserInfoFromJwt();
        
        // Assert
        assertNotNull(userInfo);
        assertEquals(expectedName, userInfo.getName());
        assertEquals(expectedEmail, userInfo.getEmail());
        assertEquals(expectedUsername, userInfo.getPreferredUsername());
        assertEquals(expectedSubject, userInfo.getSubject());
    }
}