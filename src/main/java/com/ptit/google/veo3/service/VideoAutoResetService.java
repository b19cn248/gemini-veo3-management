package com.ptit.google.veo3.service;

import com.ptit.google.veo3.entity.Video;
import com.ptit.google.veo3.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service chịu trách nhiệm tự động reset video sau khi quá thời hạn làm việc
 * 
 * BUSINESS LOGIC:
 * - Khi video được assign cho nhân viên, thời gian assignedAt sẽ được lưu
 * - Scheduled job chạy mỗi phút để kiểm tra video quá hạn (mặc định 15 phút)
 * - Video quá hạn sẽ được reset về trạng thái CHUA_AI_NHAN, assignedStaff = null
 * 
 * PERFORMANCE CONSIDERATIONS:
 * - Sử dụng composite index (assigned_at, status, is_deleted) để tối ưu query
 * - Batch processing với @Transactional để đảm bảo consistency
 * - Log chi tiết để monitoring và debugging
 * 
 * @author System
 * @since 1.4.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoAutoResetService {

    private final VideoRepository videoRepository;

    /**
     * Số phút timeout để tự động reset video
     * Có thể config từ application.yml, mặc định 15 phút
     */
    @Value("${app.video.assignment-timeout-minutes:15}")
    private int assignmentTimeoutMinutes;

    /**
     * Scheduled job chạy mỗi phút để kiểm tra và reset video quá hạn
     * 
     * Cron expression: "0 * * * * *" = chạy vào giây 0 của mỗi phút
     * 
     * TRANSACTION ISOLATION:
     * - READ_COMMITTED để tránh phantom reads
     * - Timeout 30 giây để tránh deadlock trong trường hợp có nhiều video quá hạn
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional(timeout = 30)
    public void autoResetExpiredVideos() {
        try {
            LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(assignmentTimeoutMinutes);
            
            // Đếm số video quá hạn trước khi xử lý (để logging)
            long expiredCount = videoRepository.countExpiredAssignedVideos(expiredTime);
            
            if (expiredCount == 0) {
                log.debug("Auto-reset check completed: No expired videos found");
                return;
            }
            
            log.info("Auto-reset job started: Found {} expired videos (timeout: {} minutes)", 
                    expiredCount, assignmentTimeoutMinutes);
            
            // Lấy danh sách video quá hạn
            List<Video> expiredVideos = videoRepository.findExpiredAssignedVideos(expiredTime);
            
            int resetCount = 0;
            for (Video video : expiredVideos) {
                try {
                    // Log thông tin chi tiết trước khi reset
                    log.info("Auto-resetting video ID: {}, assigned to: '{}', assigned at: {}, status: {}", 
                            video.getId(), video.getAssignedStaff(), video.getAssignedAt(), video.getStatus());
                    
                    // Thực hiện reset video
                    video.autoReset();
                    videoRepository.save(video);
                    
                    resetCount++;
                    
                    log.info("Successfully auto-reset video ID: {} from staff: '{}'", 
                            video.getId(), video.getAssignedStaff());
                    
                } catch (Exception e) {
                    log.error("Failed to auto-reset video ID: {}, assigned to: '{}': {}", 
                            video.getId(), video.getAssignedStaff(), e.getMessage(), e);
                }
            }
            
            log.info("Auto-reset job completed: Successfully reset {}/{} expired videos", 
                    resetCount, expiredVideos.size());
            
        } catch (Exception e) {
            log.error("Auto-reset job failed with error: {}", e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra video cụ thể có quá hạn không (dùng cho testing hoặc manual check)
     * 
     * @param videoId ID của video cần kiểm tra
     * @return true nếu video quá hạn và cần reset
     */
    public boolean isVideoExpired(Long videoId) {
        return videoRepository.findById(videoId)
                .map(video -> video.isAssignmentExpired(assignmentTimeoutMinutes))
                .orElse(false);
    }

    /**
     * Reset manual một video cụ thể (dùng cho testing hoặc admin action)
     * 
     * @param videoId ID của video cần reset
     * @return true nếu reset thành công
     */
    @Transactional
    public boolean manualResetVideo(Long videoId) {
        try {
            return videoRepository.findById(videoId)
                    .map(video -> {
                        if (video.getAssignedStaff() != null && !video.getAssignedStaff().trim().isEmpty()) {
                            log.info("Manual reset video ID: {}, assigned to: '{}'", 
                                    videoId, video.getAssignedStaff());
                            
                            video.autoReset();
                            videoRepository.save(video);
                            
                            log.info("Successfully manual reset video ID: {}", videoId);
                            return true;
                        }
                        return false;
                    })
                    .orElse(false);
        } catch (Exception e) {
            log.error("Failed to manual reset video ID: {}: {}", videoId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Lấy thống kê về video quá hạn (dùng cho monitoring dashboard)
     * 
     * @return Số lượng video hiện tại đang quá hạn
     */
    public long getExpiredVideoCount() {
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(assignmentTimeoutMinutes);
        return videoRepository.countExpiredAssignedVideos(expiredTime);
    }

    /**
     * Lấy danh sách video quá hạn (dùng cho admin dashboard)
     * 
     * @return Danh sách video quá hạn
     */
    public List<Video> getExpiredVideos() {
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(assignmentTimeoutMinutes);
        return videoRepository.findExpiredAssignedVideos(expiredTime);
    }
}