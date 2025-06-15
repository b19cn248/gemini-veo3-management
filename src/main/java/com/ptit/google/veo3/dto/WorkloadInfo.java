package com.ptit.google.veo3.dto;

import com.ptit.google.veo3.entity.Video;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO class chứa thông tin chi tiết về workload của nhân viên
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkloadInfo {
    private String assignedStaff;
    private long totalActive;
    private long dangLamCount;
    private long dangSuaCount;
    private long canSuaGapCount;
    private boolean canAcceptNewTask;
    private int maxConcurrentVideos;
    private List<Video> activeVideos;
    
    private static final int MAX_CONCURRENT_VIDEOS = 3;
    
    public static WorkloadInfo empty() {
        return WorkloadInfo.builder()
                .assignedStaff("")
                .totalActive(0)
                .dangLamCount(0)
                .dangSuaCount(0)
                .canSuaGapCount(0)
                .canAcceptNewTask(true)
                .maxConcurrentVideos(MAX_CONCURRENT_VIDEOS)
                .activeVideos(List.of())
                .build();
    }
}