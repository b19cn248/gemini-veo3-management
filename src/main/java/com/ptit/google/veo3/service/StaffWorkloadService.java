package com.ptit.google.veo3.service;

import com.ptit.google.veo3.dto.WorkloadInfo;
import com.ptit.google.veo3.entity.Video;
import com.ptit.google.veo3.repository.VideoRepository;
import com.ptit.google.veo3.service.interfaces.IStaffWorkloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Service chuyên xử lý logic liên quan đến workload (khối lượng công việc) của nhân viên
 * 
 * Chịu trách nhiệm:
 * - Kiểm tra số lượng video đang active của nhân viên
 * - Validate khả năng nhận thêm task mới
 * - Provide detailed breakdown của workload
 * 
 * Business Rules:
 * - Nhân viên tối đa được làm 3 video cùng lúc
 * - Video "active" bao gồm: DANG_LAM, DANG_SUA, hoặc CAN_SUA_GAP
 * 
 * @author Generated
 * @since 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StaffWorkloadService implements IStaffWorkloadService {

    private final VideoRepository videoRepository;
    
    /**
     * Giới hạn tối đa số video một nhân viên có thể làm cùng lúc
     */
    private static final int MAX_CONCURRENT_VIDEOS = 3;
    
    /**
     * Kiểm tra xem nhân viên có thể nhận thêm task mới không
     * 
     * @param assignedStaff Tên nhân viên cần kiểm tra
     * @return true nếu có thể nhận thêm task, false nếu đã đạt giới hạn
     * @throws IllegalArgumentException nếu assignedStaff null hoặc empty
     */
    public boolean canAcceptNewTask(String assignedStaff) {
        if (!StringUtils.hasText(assignedStaff)) {
            throw new IllegalArgumentException("Tên nhân viên không được để trống");
        }
        
        String trimmedStaff = assignedStaff.trim();
        long currentWorkload = videoRepository.countActiveWorkloadByAssignedStaff(trimmedStaff);
        
        boolean canAccept = currentWorkload < MAX_CONCURRENT_VIDEOS;
        
        log.debug("Workload check for staff '{}': current={}, max={}, canAccept={}", 
                trimmedStaff, currentWorkload, MAX_CONCURRENT_VIDEOS, canAccept);
                
        return canAccept;
    }
    
    /**
     * Lấy số lượng video đang active của nhân viên
     * 
     * @param assignedStaff Tên nhân viên
     * @return Số lượng video đang active
     */
    public long getCurrentWorkload(String assignedStaff) {
        if (!StringUtils.hasText(assignedStaff)) {
            return 0;
        }
        
        return videoRepository.countActiveWorkloadByAssignedStaff(assignedStaff.trim());
    }
    
    /**
     * Validate và throw exception nếu nhân viên không thể nhận thêm task
     * 
     * @param assignedStaff Tên nhân viên cần validate
     * @throws IllegalArgumentException nếu không thể nhận thêm task
     */
    public void validateCanAcceptNewTask(String assignedStaff) {
        if (!StringUtils.hasText(assignedStaff)) {
            // Cho phép unassign (assignedStaff = null)
            return;
        }
        
        String trimmedStaff = assignedStaff.trim();
        long currentWorkload = getCurrentWorkload(trimmedStaff);
        
        if (currentWorkload >= MAX_CONCURRENT_VIDEOS) {
            // Lấy chi tiết các video để thông báo cụ thể
            List<Video> activeVideos = videoRepository.findActiveWorkloadByAssignedStaff(trimmedStaff);
            
            String errorMessage = String.format(
                "Nhân viên '%s' đã đạt giới hạn tối đa %d video. " +
                "Hiện tại đang xử lý %d video. " +
                "Vui lòng hoàn thành một số video trước khi nhận task mới.",
                trimmedStaff, MAX_CONCURRENT_VIDEOS, currentWorkload
            );
            
            // Log chi tiết để debugging
            log.warn("Staff '{}' workload exceeded: {}/{} videos. Active videos: {}", 
                    trimmedStaff, currentWorkload, MAX_CONCURRENT_VIDEOS, 
                    activeVideos.stream()
                            .map(v -> String.format("ID:%d(Status:%s,Delivery:%s)", 
                                    v.getId(), v.getStatus(), v.getDeliveryStatus()))
                            .toList());
            
            throw new IllegalArgumentException(errorMessage);
        }
    }
    
    /**
     * Lấy thông tin chi tiết về workload của nhân viên
     * 
     * @param assignedStaff Tên nhân viên
     * @return WorkloadInfo chứa thông tin chi tiết
     */
    public WorkloadInfo getWorkloadInfo(String assignedStaff) {
        if (!StringUtils.hasText(assignedStaff)) {
            return WorkloadInfo.empty();
        }
        
        String trimmedStaff = assignedStaff.trim();
        List<Video> activeVideos = videoRepository.findActiveWorkloadByAssignedStaff(trimmedStaff);
        
        long dangLamCount = activeVideos.stream()
                .filter(v -> "DANG_LAM".equals(v.getStatus().name()))
                .count();
                
        long dangSuaCount = activeVideos.stream()
                .filter(v -> "DANG_SUA".equals(v.getStatus().name()))
                .count();
                
        long canSuaGapCount = activeVideos.stream()
                .filter(v -> "CAN_SUA_GAP".equals(v.getDeliveryStatus().name()))
                .count();
        
        return WorkloadInfo.builder()
                .assignedStaff(trimmedStaff)
                .totalActive(activeVideos.size())
                .dangLamCount(dangLamCount)
                .dangSuaCount(dangSuaCount)
                .canSuaGapCount(canSuaGapCount)
                .canAcceptNewTask(activeVideos.size() < MAX_CONCURRENT_VIDEOS)
                .maxConcurrentVideos(MAX_CONCURRENT_VIDEOS)
                .activeVideos(activeVideos)
                .build();
    }
    
}