package com.ptit.google.veo3.service;

import com.ptit.google.veo3.dto.VideoResponseDto;
import com.ptit.google.veo3.entity.Video;
import com.ptit.google.veo3.entity.VideoStatus;
import com.ptit.google.veo3.repository.VideoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho Video Cancel functionality
 */
@ExtendWith(MockitoExtension.class)
class VideoCancelServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private StaffWorkloadService staffWorkloadService;

    @InjectMocks
    private VideoService videoService;

    @Test
    void testCancelVideo_WithAdminUser_ShouldResetVideoSuccessfully() {
        // Given
        Long videoId = 1L;
        Video assignedVideo = Video.builder()
                .id(videoId)
                .customerName("Test Customer")
                .assignedStaff("John Doe")
                .status(VideoStatus.DANG_LAM)
                .assignedAt(LocalDateTime.now().minusMinutes(30))
                .build();

        when(jwtTokenService.isCurrentUserAdmin()).thenReturn(true);
        when(jwtTokenService.getCurrentUserNameFromJwt()).thenReturn("admin@company.com");
        when(videoRepository.findById(videoId)).thenReturn(Optional.of(assignedVideo));
        when(videoRepository.save(any(Video.class))).thenReturn(assignedVideo);

        // When
        VideoResponseDto result = videoService.cancelVideo(videoId);

        // Then
        assertNotNull(result);
        verify(videoRepository).save(assignedVideo);
        
        // Verify video was reset
        assertNull(assignedVideo.getAssignedStaff());
        assertNull(assignedVideo.getAssignedAt());
        assertEquals(VideoStatus.CHUA_AI_NHAN, assignedVideo.getStatus());
    }

    @Test
    void testCancelVideo_WithNonAdminUser_ShouldThrowSecurityException() {
        // Given
        Long videoId = 1L;
        
        when(jwtTokenService.isCurrentUserAdmin()).thenReturn(false);
        when(jwtTokenService.getCurrentUserNameFromJwt()).thenReturn("normaluser@company.com");

        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            videoService.cancelVideo(videoId);
        });

        assertEquals("Chỉ có admin mới có quyền hủy video", exception.getMessage());
        verify(videoRepository, never()).findById(any());
        verify(videoRepository, never()).save(any());
    }

    @Test
    void testCancelVideo_WithNonExistentVideo_ShouldThrowVideoNotFoundException() {
        // Given
        Long videoId = 999L;
        
        when(jwtTokenService.isCurrentUserAdmin()).thenReturn(true);
        when(videoRepository.findById(videoId)).thenReturn(Optional.empty());

        // When & Then
        VideoService.VideoNotFoundException exception = assertThrows(
                VideoService.VideoNotFoundException.class, () -> {
                    videoService.cancelVideo(videoId);
                });

        assertEquals("Không tìm thấy video với ID: " + videoId, exception.getMessage());
        verify(videoRepository, never()).save(any());
    }

    @Test
    void testCancelVideo_WithAlreadyUnassignedVideo_ShouldStillWork() {
        // Given
        Long videoId = 1L;
        Video unassignedVideo = Video.builder()
                .id(videoId)
                .customerName("Test Customer")
                .assignedStaff(null)
                .status(VideoStatus.CHUA_AI_NHAN)
                .assignedAt(null)
                .build();

        when(jwtTokenService.isCurrentUserAdmin()).thenReturn(true);
        when(jwtTokenService.getCurrentUserNameFromJwt()).thenReturn("admin@company.com");
        when(videoRepository.findById(videoId)).thenReturn(Optional.of(unassignedVideo));
        when(videoRepository.save(any(Video.class))).thenReturn(unassignedVideo);

        // When
        VideoResponseDto result = videoService.cancelVideo(videoId);

        // Then
        assertNotNull(result);
        verify(videoRepository).save(unassignedVideo);
        
        // Verify video remains reset
        assertNull(unassignedVideo.getAssignedStaff());
        assertNull(unassignedVideo.getAssignedAt());
        assertEquals(VideoStatus.CHUA_AI_NHAN, unassignedVideo.getStatus());
    }
}
