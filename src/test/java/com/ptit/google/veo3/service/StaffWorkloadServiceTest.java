package com.ptit.google.veo3.service;

import com.ptit.google.veo3.entity.DeliveryStatus;
import com.ptit.google.veo3.entity.Video;
import com.ptit.google.veo3.entity.VideoStatus;
import com.ptit.google.veo3.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho StaffWorkloadService
 * 
 * Test cases:
 * 1. Workload calculation với các trạng thái khác nhau
 * 2. Validation logic cho max concurrent videos (3)
 * 3. Edge cases và error scenarios
 * 4. WorkloadInfo detailed breakdown
 * 
 * @author Generated
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class StaffWorkloadServiceTest {

    @Mock
    private VideoRepository videoRepository;
    
    private StaffWorkloadService staffWorkloadService;
    
    @BeforeEach
    void setUp() {
        staffWorkloadService = new StaffWorkloadService(videoRepository);
    }
    
    @Test
    void canAcceptNewTask_WithZeroActiveVideos_ReturnsTrue() {
        // Arrange
        String staffName = "Nguyễn Minh Hiếu";
        when(videoRepository.countActiveWorkloadByAssignedStaff(staffName)).thenReturn(0L);
        
        // Act
        boolean canAccept = staffWorkloadService.canAcceptNewTask(staffName);
        
        // Assert
        assertTrue(canAccept);
        verify(videoRepository).countActiveWorkloadByAssignedStaff(staffName);
    }
    
    @Test
    void canAcceptNewTask_WithTwoActiveVideos_ReturnsTrue() {
        // Arrange
        String staffName = "Nguyễn Minh Hiếu";
        when(videoRepository.countActiveWorkloadByAssignedStaff(staffName)).thenReturn(2L);
        
        // Act
        boolean canAccept = staffWorkloadService.canAcceptNewTask(staffName);
        
        // Assert
        assertTrue(canAccept);
        verify(videoRepository).countActiveWorkloadByAssignedStaff(staffName);
    }
    
    @Test
    void canAcceptNewTask_WithThreeActiveVideos_ReturnsFalse() {
        // Arrange
        String staffName = "Nguyễn Minh Hiếu";
        when(videoRepository.countActiveWorkloadByAssignedStaff(staffName)).thenReturn(3L);
        
        // Act
        boolean canAccept = staffWorkloadService.canAcceptNewTask(staffName);
        
        // Assert
        assertFalse(canAccept);
        verify(videoRepository).countActiveWorkloadByAssignedStaff(staffName);
    }
    
    @Test
    void canAcceptNewTask_WithMoreThanThreeActiveVideos_ReturnsFalse() {
        // Arrange
        String staffName = "Nguyễn Minh Hiếu";
        when(videoRepository.countActiveWorkloadByAssignedStaff(staffName)).thenReturn(5L);
        
        // Act
        boolean canAccept = staffWorkloadService.canAcceptNewTask(staffName);
        
        // Assert
        assertFalse(canAccept);
        verify(videoRepository).countActiveWorkloadByAssignedStaff(staffName);
    }
    
    @Test
    void canAcceptNewTask_WithNullStaffName_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> staffWorkloadService.canAcceptNewTask(null)
        );
        
        assertEquals("Tên nhân viên không được để trống", exception.getMessage());
        verifyNoInteractions(videoRepository);
    }
    
    @Test
    void canAcceptNewTask_WithEmptyStaffName_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> staffWorkloadService.canAcceptNewTask("   ")
        );
        
        assertEquals("Tên nhân viên không được để trống", exception.getMessage());
        verifyNoInteractions(videoRepository);
    }
    
    @Test
    void getCurrentWorkload_WithValidStaff_ReturnsCorrectCount() {
        // Arrange
        String staffName = "Nguyễn Minh Hiếu";
        when(videoRepository.countActiveWorkloadByAssignedStaff(staffName)).thenReturn(2L);
        
        // Act
        long workload = staffWorkloadService.getCurrentWorkload(staffName);
        
        // Assert
        assertEquals(2L, workload);
        verify(videoRepository).countActiveWorkloadByAssignedStaff(staffName);
    }
    
    @Test
    void getCurrentWorkload_WithNullStaff_ReturnsZero() {
        // Act
        long workload = staffWorkloadService.getCurrentWorkload(null);
        
        // Assert
        assertEquals(0L, workload);
        verifyNoInteractions(videoRepository);
    }
    
    @Test
    void getCurrentWorkload_WithEmptyStaff_ReturnsZero() {
        // Act
        long workload = staffWorkloadService.getCurrentWorkload("   ");
        
        // Assert
        assertEquals(0L, workload);
        verifyNoInteractions(videoRepository);
    }
    
    @Test
    void validateCanAcceptNewTask_WithValidWorkload_DoesNotThrow() {
        // Arrange
        String staffName = "Nguyễn Minh Hiếu";
        when(videoRepository.countActiveWorkloadByAssignedStaff(staffName)).thenReturn(2L);
        
        // Act & Assert
        assertDoesNotThrow(() -> staffWorkloadService.validateCanAcceptNewTask(staffName));
        verify(videoRepository).countActiveWorkloadByAssignedStaff(staffName);
    }
    
    @Test
    void validateCanAcceptNewTask_WithMaxWorkload_ThrowsException() {
        // Arrange
        String staffName = "Nguyễn Minh Hiếu";
        when(videoRepository.countActiveWorkloadByAssignedStaff(staffName)).thenReturn(3L);
        when(videoRepository.findActiveWorkloadByAssignedStaff(staffName)).thenReturn(
            createMockVideos(3)
        );
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> staffWorkloadService.validateCanAcceptNewTask(staffName)
        );
        
        assertTrue(exception.getMessage().contains("đã đạt giới hạn tối đa 3 video"));
        assertTrue(exception.getMessage().contains("Hiện tại đang xử lý 3 video"));
        assertTrue(exception.getMessage().contains(staffName));
    }
    
    @Test
    void validateCanAcceptNewTask_WithNullStaff_DoesNotThrow() {
        // Act & Assert (null staff means unassign, which should be allowed)
        assertDoesNotThrow(() -> staffWorkloadService.validateCanAcceptNewTask(null));
        verifyNoInteractions(videoRepository);
    }
    
    @Test
    void validateCanAcceptNewTask_WithEmptyStaff_DoesNotThrow() {
        // Act & Assert (empty staff means unassign, which should be allowed)
        assertDoesNotThrow(() -> staffWorkloadService.validateCanAcceptNewTask("   "));
        verifyNoInteractions(videoRepository);
    }
    
    @Test
    void getWorkloadInfo_WithMixedVideoStatuses_ReturnsCorrectBreakdown() {
        // Arrange
        String staffName = "Nguyễn Minh Hiếu";
        List<Video> activeVideos = Arrays.asList(
            createVideoWithStatus(1L, VideoStatus.DANG_LAM, DeliveryStatus.CHUA_GUI),
            createVideoWithStatus(2L, VideoStatus.DANG_SUA, DeliveryStatus.CHUA_GUI),
            createVideoWithStatus(3L, VideoStatus.DA_XONG, DeliveryStatus.CAN_SUA_GAP)
        );
        
        when(videoRepository.findActiveWorkloadByAssignedStaff(staffName)).thenReturn(activeVideos);
        
        // Act
        StaffWorkloadService.WorkloadInfo workloadInfo = staffWorkloadService.getWorkloadInfo(staffName);
        
        // Assert
        assertEquals(staffName, workloadInfo.getAssignedStaff());
        assertEquals(3, workloadInfo.getTotalActive());
        assertEquals(1, workloadInfo.getDangLamCount());
        assertEquals(1, workloadInfo.getDangSuaCount());
        assertEquals(1, workloadInfo.getCanSuaGapCount());
        assertFalse(workloadInfo.isCanAcceptNewTask()); // 3 videos = max limit
        assertEquals(3, workloadInfo.getMaxConcurrentVideos());
        assertEquals(3, workloadInfo.getActiveVideos().size());
    }
    
    @Test
    void getWorkloadInfo_WithNoActiveVideos_ReturnsEmptyBreakdown() {
        // Arrange
        String staffName = "Nguyễn Minh Hiếu";
        when(videoRepository.findActiveWorkloadByAssignedStaff(staffName)).thenReturn(List.of());
        
        // Act
        StaffWorkloadService.WorkloadInfo workloadInfo = staffWorkloadService.getWorkloadInfo(staffName);
        
        // Assert
        assertEquals(staffName, workloadInfo.getAssignedStaff());
        assertEquals(0, workloadInfo.getTotalActive());
        assertEquals(0, workloadInfo.getDangLamCount());
        assertEquals(0, workloadInfo.getDangSuaCount());
        assertEquals(0, workloadInfo.getCanSuaGapCount());
        assertTrue(workloadInfo.isCanAcceptNewTask());
        assertEquals(3, workloadInfo.getMaxConcurrentVideos());
        assertTrue(workloadInfo.getActiveVideos().isEmpty());
    }
    
    @Test
    void getWorkloadInfo_WithNullStaff_ReturnsEmptyInfo() {
        // Act
        StaffWorkloadService.WorkloadInfo workloadInfo = staffWorkloadService.getWorkloadInfo(null);
        
        // Assert
        assertEquals("", workloadInfo.getAssignedStaff());
        assertEquals(0, workloadInfo.getTotalActive());
        assertTrue(workloadInfo.isCanAcceptNewTask());
        assertTrue(workloadInfo.getActiveVideos().isEmpty());
        verifyNoInteractions(videoRepository);
    }
    
    @Test
    void getWorkloadInfo_WithEmptyStaff_ReturnsEmptyInfo() {
        // Act
        StaffWorkloadService.WorkloadInfo workloadInfo = staffWorkloadService.getWorkloadInfo("   ");
        
        // Assert
        assertEquals("", workloadInfo.getAssignedStaff());
        assertEquals(0, workloadInfo.getTotalActive());
        assertTrue(workloadInfo.isCanAcceptNewTask());
        assertTrue(workloadInfo.getActiveVideos().isEmpty());
        verifyNoInteractions(videoRepository);
    }
    
    // Helper methods
    private List<Video> createMockVideos(int count) {
        return List.of(); // Simplified for test - actual implementation would create mock Video objects
    }
    
    private Video createVideoWithStatus(Long id, VideoStatus status, DeliveryStatus deliveryStatus) {
        Video video = new Video();
        video.setId(id);
        video.setStatus(status);
        video.setDeliveryStatus(deliveryStatus);
        video.setAssignedStaff("Nguyễn Minh Hiếu");
        return video;
    }
}