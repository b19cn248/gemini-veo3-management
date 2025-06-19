package com.ptit.google.veo3.mapper;

import com.ptit.google.veo3.dto.WorkloadInfo;
import com.ptit.google.veo3.entity.Video;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StaffWorkloadMapper {

    private final VideoMapper videoMapper;

    @Autowired
    public StaffWorkloadMapper(VideoMapper videoMapper) {
        this.videoMapper = videoMapper;
    }

    public WorkloadInfo buildWorkloadInfo(String assignedStaff, 
                                         List<Video> activeVideos,
                                         long dangLamCount,
                                         long dangSuaCount,
                                         long canSuaGapCount,
                                         int maxConcurrentVideos) {
        
        long totalActive = activeVideos != null ? activeVideos.size() : 0;
        boolean canAcceptNewTask = totalActive < maxConcurrentVideos;
        
        return WorkloadInfo.builder()
                .assignedStaff(assignedStaff)
                .totalActive(totalActive)
                .dangLamCount(dangLamCount)
                .dangSuaCount(dangSuaCount)
                .canSuaGapCount(canSuaGapCount)
                .canAcceptNewTask(canAcceptNewTask)
                .maxConcurrentVideos(maxConcurrentVideos)
                .activeVideos(activeVideos)
                .build();
    }

    public WorkloadInfo buildWorkloadInfoFromVideos(String assignedStaff, 
                                                   List<Video> allVideos,
                                                   int maxConcurrentVideos) {
        if (allVideos == null) {
            return WorkloadInfo.empty();
        }

        List<Video> activeVideos = allVideos.stream()
                .filter(v -> !isCompletedStatus(v))
                .toList();

        long dangLamCount = allVideos.stream()
                .filter(this::isDangLamStatus)
                .count();

        long dangSuaCount = allVideos.stream()
                .filter(this::isDangSuaStatus)
                .count();

        long canSuaGapCount = allVideos.stream()
                .filter(this::isCanSuaGapStatus)
                .count();

        return buildWorkloadInfo(
                assignedStaff,
                activeVideos,
                dangLamCount,
                dangSuaCount,
                canSuaGapCount,
                maxConcurrentVideos
        );
    }

    public WorkloadInfo createEmptyWorkloadInfo(String assignedStaff) {
        return WorkloadInfo.builder()
                .assignedStaff(assignedStaff)
                .totalActive(0L)
                .dangLamCount(0L)
                .dangSuaCount(0L)
                .canSuaGapCount(0L)
                .canAcceptNewTask(true)
                .maxConcurrentVideos(3) // Default max concurrent videos
                .activeVideos(List.of())
                .build();
    }

    private boolean isCompletedStatus(Video video) {
        if (video == null || video.getStatus() == null) {
            return false;
        }
        
        return switch (video.getStatus()) {
            case DA_XONG, DA_SUA_XONG -> true;
            default -> false;
        };
    }

    private boolean isDangLamStatus(Video video) {
        if (video == null || video.getStatus() == null) {
            return false;
        }
        
        return switch (video.getStatus()) {
            case DANG_LAM -> true;
            default -> false;
        };
    }

    private boolean isDangSuaStatus(Video video) {
        if (video == null || video.getStatus() == null) {
            return false;
        }
        
        return switch (video.getStatus()) {
            case DANG_SUA -> true;
            default -> false;
        };
    }

    private boolean isCanSuaGapStatus(Video video) {
        if (video == null || video.getStatus() == null) {
            return false;
        }
        
        // Based on business logic, videos that need urgent editing
        // This could be videos with issues or requiring immediate attention
        // Since CAN_SUA_GAP doesn't exist in enum, we'll check for videos that might need urgent editing
        return switch (video.getStatus()) {
            case DANG_SUA -> isUrgentEdit(video);
            default -> false;
        };
    }
    
    private boolean isUrgentEdit(Video video) {
        // Business logic to determine if a video needs urgent editing
        // This could be based on delivery time, customer priority, etc.
        if (video.getDeliveryTime() != null) {
            return video.getDeliveryTime().isBefore(java.time.LocalDateTime.now().plusHours(24));
        }
        return false;
    }
}