package com.ptit.google.veo3.integration;

import com.ptit.google.veo3.entity.DeliveryStatus;
import com.ptit.google.veo3.entity.Video;
import com.ptit.google.veo3.entity.VideoStatus;
import com.ptit.google.veo3.repository.VideoRepository;
import com.ptit.google.veo3.service.StaffWorkloadService;
import com.ptit.google.veo3.service.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests để verify workload logic hoạt động đúng với database thật
 * 
 * Tests cover:
 * 1. Database query accuracy cho workload calculation
 * 2. Repository method integration
 * 3. Service layer interaction với real data
 * 4. Complex scenarios với mixed statuses
 * 
 * @author Generated
 * @since 1.0
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({StaffWorkloadService.class})
class StaffWorkloadIntegrationTest {

    @Autowired
    private VideoRepository videoRepository;
    
    @Autowired
    private StaffWorkloadService staffWorkloadService;
    
    private static final String STAFF_NAME = "Nguyễn Minh Hiếu";
    private static final String OTHER_STAFF = "Trần Văn Nam";
    
    @BeforeEach
    void setUp() {
        // Clear all data trước mỗi test
        videoRepository.deleteAll();
    }
    
    @Test
    void workloadCalculation_WithMixedStatuses_CalculatesCorrectly() {
        // Arrange: Tạo videos với các status khác nhau
        saveVideo("Customer 1", STAFF_NAME, VideoStatus.DANG_LAM, DeliveryStatus.CHUA_GUI);
        saveVideo("Customer 2", STAFF_NAME, VideoStatus.DANG_SUA, DeliveryStatus.CHUA_GUI);  
        saveVideo("Customer 3", STAFF_NAME, VideoStatus.DA_XONG, DeliveryStatus.CAN_SUA_GAP);
        
        // Video không thuộc staff này
        saveVideo("Customer 4", OTHER_STAFF, VideoStatus.DANG_LAM, DeliveryStatus.CHUA_GUI);
        
        // Video đã hoàn thành hoàn toàn (không count)
        saveVideo("Customer 5", STAFF_NAME, VideoStatus.DA_XONG, DeliveryStatus.DA_GUI);
        
        // Act
        long workload = staffWorkloadService.getCurrentWorkload(STAFF_NAME);
        StaffWorkloadService.WorkloadInfo info = staffWorkloadService.getWorkloadInfo(STAFF_NAME);
        
        // Assert
        assertEquals(3L, workload, "Should count DANG_LAM + DANG_SUA + CAN_SUA_GAP");
        assertEquals(3, info.getTotalActive());
        assertEquals(1, info.getDangLamCount());
        assertEquals(1, info.getDangSuaCount());
        assertEquals(1, info.getCanSuaGapCount());
        assertFalse(info.isCanAcceptNewTask(), "Should not accept new task at limit");
    }
    
    @Test
    void workloadCalculation_WithDuplicateConditions_CountsOnlyOnce() {
        // Arrange: Video vừa DANG_SUA vừa CAN_SUA_GAP (chỉ count 1 lần)
        Video video = saveVideo("Customer 1", STAFF_NAME, VideoStatus.DANG_SUA, DeliveryStatus.CAN_SUA_GAP);
        
        // Act
        long workload = staffWorkloadService.getCurrentWorkload(STAFF_NAME);
        StaffWorkloadService.WorkloadInfo info = staffWorkloadService.getWorkloadInfo(STAFF_NAME);
        
        // Assert
        assertEquals(1L, workload, "Should count video only once despite matching multiple conditions");
        assertEquals(1, info.getTotalActive());
        assertEquals(0, info.getDangLamCount());
        assertEquals(1, info.getDangSuaCount());
        assertEquals(1, info.getCanSuaGapCount());
        assertTrue(info.isCanAcceptNewTask(), "Should accept new task when under limit");
    }
    
    @Test
    void canAcceptNewTask_AtLimitBoundary_ReturnsCorrectResult() {
        // Arrange: Tạo đúng 3 videos (at limit)
        saveVideo("Customer 1", STAFF_NAME, VideoStatus.DANG_LAM, DeliveryStatus.CHUA_GUI);
        saveVideo("Customer 2", STAFF_NAME, VideoStatus.DANG_LAM, DeliveryStatus.CHUA_GUI);
        saveVideo("Customer 3", STAFF_NAME, VideoStatus.DANG_LAM, DeliveryStatus.CHUA_GUI);
        
        // Act & Assert
        assertFalse(staffWorkloadService.canAcceptNewTask(STAFF_NAME), "At limit - should not accept");
        
        // Remove one video
        videoRepository.deleteAll();
        saveVideo("Customer 1", STAFF_NAME, VideoStatus.DANG_LAM, DeliveryStatus.CHUA_GUI);
        saveVideo("Customer 2", STAFF_NAME, VideoStatus.DANG_LAM, DeliveryStatus.CHUA_GUI);
        
        assertTrue(staffWorkloadService.canAcceptNewTask(STAFF_NAME), "Under limit - should accept");
    }
    
    @Test
    void validateCanAcceptNewTask_WithExceededWorkload_ThrowsDetailedException() {
        // Arrange: Tạo 4 videos (exceed limit)
        saveVideo("Customer 1", STAFF_NAME, VideoStatus.DANG_LAM, DeliveryStatus.CHUA_GUI);
        saveVideo("Customer 2", STAFF_NAME, VideoStatus.DANG_SUA, DeliveryStatus.CHUA_GUI);
        saveVideo("Customer 3", STAFF_NAME, VideoStatus.DA_XONG, DeliveryStatus.CAN_SUA_GAP);
        saveVideo("Customer 4", STAFF_NAME, VideoStatus.DANG_LAM, DeliveryStatus.CHUA_GUI);
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> staffWorkloadService.validateCanAcceptNewTask(STAFF_NAME)
        );
        
        String errorMessage = exception.getMessage();
        assertTrue(errorMessage.contains(STAFF_NAME));
        assertTrue(errorMessage.contains("đã đạt giới hạn tối đa 3 video"));
        assertTrue(errorMessage.contains("Hiện tại đang xử lý 4 video"));
    }
    
    @Test
    void workloadCalculation_WithDeletedVideos_IgnoresDeletedRecords() {
        // Arrange: Tạo videos, một số bị xóa mềm
        Video activeVideo = saveVideo("Customer 1", STAFF_NAME, VideoStatus.DANG_LAM, DeliveryStatus.CHUA_GUI);
        Video deletedVideo = saveVideo("Customer 2", STAFF_NAME, VideoStatus.DANG_LAM, DeliveryStatus.CHUA_GUI);
        
        // Soft delete một video
        deletedVideo.setIsDeleted(true);
        videoRepository.save(deletedVideo);
        
        // Act
        long workload = staffWorkloadService.getCurrentWorkload(STAFF_NAME);
        
        // Assert
        assertEquals(1L, workload, "Should ignore soft-deleted videos");
        assertTrue(staffWorkloadService.canAcceptNewTask(STAFF_NAME));
    }
    
    @Test
    void workloadCalculation_WithEmptyDatabase_ReturnsZero() {
        // Act
        long workload = staffWorkloadService.getCurrentWorkload(STAFF_NAME);
        StaffWorkloadService.WorkloadInfo info = staffWorkloadService.getWorkloadInfo(STAFF_NAME);
        
        // Assert
        assertEquals(0L, workload);
        assertEquals(0, info.getTotalActive());
        assertTrue(info.isCanAcceptNewTask());
        assertTrue(info.getActiveVideos().isEmpty());
    }
    
    @Test
    void workloadCalculation_WithOnlyCompletedVideos_ReturnsZero() {
        // Arrange: Chỉ có videos đã hoàn thành
        saveVideo("Customer 1", STAFF_NAME, VideoStatus.DA_XONG, DeliveryStatus.DA_GUI);
        saveVideo("Customer 2", STAFF_NAME, VideoStatus.DA_SUA_XONG, DeliveryStatus.DA_GUI);
        saveVideo("Customer 3", STAFF_NAME, VideoStatus.CHUA_AI_NHAN, DeliveryStatus.CHUA_GUI);
        
        // Act
        long workload = staffWorkloadService.getCurrentWorkload(STAFF_NAME);
        
        // Assert
        assertEquals(0L, workload, "Completed videos should not count toward workload");
        assertTrue(staffWorkloadService.canAcceptNewTask(STAFF_NAME));
    }
    
    @Test
    void repositoryQuery_countActiveWorkloadByAssignedStaff_IsAccurate() {
        // Arrange: Setup complex scenario
        saveVideo("Customer 1", STAFF_NAME, VideoStatus.DANG_LAM, DeliveryStatus.CHUA_GUI);      // Count: 1
        saveVideo("Customer 2", STAFF_NAME, VideoStatus.DANG_SUA, DeliveryStatus.CAN_SUA_GAP);  // Count: 1 (not 2)
        saveVideo("Customer 3", STAFF_NAME, VideoStatus.DA_XONG, DeliveryStatus.CAN_SUA_GAP);   // Count: 1
        saveVideo("Customer 4", STAFF_NAME, VideoStatus.DA_XONG, DeliveryStatus.DA_GUI);        // Count: 0
        saveVideo("Customer 5", OTHER_STAFF, VideoStatus.DANG_LAM, DeliveryStatus.CHUA_GUI);    // Count: 0
        
        // Act: Test repository method directly
        long repoCount = videoRepository.countActiveWorkloadByAssignedStaff(STAFF_NAME);
        
        // Assert
        assertEquals(3L, repoCount, "Repository should return correct active count");
    }
    
    // Helper methods
    private Video saveVideo(String customerName, String assignedStaff, VideoStatus status, DeliveryStatus deliveryStatus) {
        Video video = Video.builder()
                .customerName(customerName)
                .assignedStaff(assignedStaff)
                .status(status)
                .deliveryStatus(deliveryStatus)
                .isDeleted(false)
                .build();
        return videoRepository.save(video);
    }
}