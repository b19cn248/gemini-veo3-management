package com.ptit.google.veo3.service;

import com.ptit.google.veo3.dto.StaffSalaryDto;
import com.ptit.google.veo3.dto.VideoRequestDto;
import com.ptit.google.veo3.dto.VideoResponseDto;
import com.ptit.google.veo3.dto.SalesSalaryDto;
import com.ptit.google.veo3.dto.SalesSalaryProjection;
import com.ptit.google.veo3.entity.AuditAction;
import com.ptit.google.veo3.entity.DeliveryStatus;
import com.ptit.google.veo3.entity.PaymentStatus;
import com.ptit.google.veo3.entity.Video;
import com.ptit.google.veo3.entity.VideoStatus;
import com.ptit.google.veo3.repository.VideoRepository;
import com.ptit.google.veo3.util.VideoPricingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service class chứa business logic để xử lý các operations liên quan đến Video
 * Tuân theo Single Responsibility Principle
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VideoService {

    private final VideoRepository videoRepository;
    private final JwtTokenService jwtTokenService;
    private final StaffWorkloadService staffWorkloadService;
    private final AuditService auditService;
    /**
     * Tạo mới một video record
     * 
     * BUSINESS LOGIC UPDATED:
     * - Tự động tính price dựa trên orderValue
     * - Validate pricing consistency
     * - Log pricing information cho audit trail
     */
    @Transactional
    public VideoResponseDto createVideo(VideoRequestDto requestDto) {
        log.info("Creating new video for customer: {}", requestDto.getCustomerName());

        Video video = mapToEntity(requestDto);
        
        // PRICING LOGIC: Tự động tính price dựa trên orderValue
        if (video.getOrderValue() != null) {
            BigDecimal calculatedPrice = VideoPricingUtil.calculatePrice(video.getOrderValue());
            
            if (calculatedPrice != null) {
                video.setPrice(calculatedPrice);
                log.info("Auto-calculated price {} for order value {} (customer: {})", 
                        calculatedPrice, video.getOrderValue(), requestDto.getCustomerName());
            } else {
                log.warn("No pricing rule found for order value {} (customer: {})", 
                        video.getOrderValue(), requestDto.getCustomerName());
                
                // Nếu client gửi price manual và không có auto-calculation, sử dụng price từ request
                if (requestDto.getPrice() != null) {
                    video.setPrice(requestDto.getPrice());
                    log.info("Using manual price {} for order value {} (customer: {})", 
                            requestDto.getPrice(), video.getOrderValue(), requestDto.getCustomerName());
                }
            }
        } else if (requestDto.getPrice() != null) {
            // Nếu không có orderValue nhưng có price, sử dụng price từ request
            video.setPrice(requestDto.getPrice());
            log.info("Using manual price {} without order value (customer: {})", 
                    requestDto.getPrice(), requestDto.getCustomerName());
        }
        
        Video savedVideo = videoRepository.save(video);

        log.info("Video created successfully with ID: {}, order value: {}, price: {}", 
                savedVideo.getId(), savedVideo.getOrderValue(), savedVideo.getPrice());
        return mapToResponseDto(savedVideo);
    }
    /**
     * Cập nhật thông tin video
     * 
     * BUSINESS LOGIC UPDATED:
     * - Recalculate price khi orderValue thay đổi
     * - Preserve existing price nếu không có rule tương ứng
     * - Log pricing changes cho audit trail
     */
    @Transactional
    public VideoResponseDto updateVideo(Long id, VideoRequestDto requestDto) {
        log.info("Updating video with ID: {}", id);

        Video existingVideo = findVideoByIdOrThrow(id);
        
        // Lưu lại giá trị cũ để logging
        BigDecimal oldOrderValue = existingVideo.getOrderValue();
        BigDecimal oldPrice = existingVideo.getPrice();
        
        updateVideoFields(existingVideo, requestDto);
        
        // PRICING LOGIC: Recalculate price nếu orderValue thay đổi
        BigDecimal newOrderValue = existingVideo.getOrderValue();
        if (newOrderValue != null && !newOrderValue.equals(oldOrderValue)) {
            BigDecimal calculatedPrice = VideoPricingUtil.calculatePrice(newOrderValue);
            
            if (calculatedPrice != null) {
                existingVideo.setPrice(calculatedPrice);
                log.info("Updated price from {} to {} due to order value change from {} to {} (video ID: {})", 
                        oldPrice, calculatedPrice, oldOrderValue, newOrderValue, id);
            } else {
                log.warn("No pricing rule found for new order value {} (video ID: {}), keeping existing price {}", 
                        newOrderValue, id, existingVideo.getPrice());
            }
        }
        
        Video updatedVideo = videoRepository.save(existingVideo);

        log.info("Video updated successfully with ID: {}, order value: {}, price: {}", 
                id, updatedVideo.getOrderValue(), updatedVideo.getPrice());
        return mapToResponseDto(updatedVideo);
    }
    /**
     * Cập nhật nhân viên được giao cho video
     * 
     * BUSINESS RULES UPDATED:
     * - Nhân viên tối đa được xử lý 3 video cùng lúc (tăng từ 2)
     * - Video "active" bao gồm: DANG_LAM, DANG_SUA, hoặc CAN_SUA_GAP
     * - Sử dụng StaffWorkloadService để kiểm tra workload tổng thể
     * 
     * @param id Video ID cần cập nhật
     * @param assignedStaff Tên nhân viên được giao mới
     * @return VideoResponseDto sau khi cập nhật
     * @throws IllegalArgumentException nếu nhân viên đã đạt giới hạn workload
     */
    @Transactional
    public VideoResponseDto updateAssignedStaff(Long id, String assignedStaff) {
        log.info("Updating assigned staff for video ID: {} to staff: {}", id, assignedStaff);

        Video existingVideo = findVideoByIdOrThrow(id);

        // Validate assigned staff length
        if (assignedStaff != null && assignedStaff.trim().length() > 255) {
            throw new IllegalArgumentException("Tên nhân viên không được vượt quá 255 ký tự");
        }

        // NEW WORKLOAD VALIDATION LOGIC
        // Kiểm tra workload tổng thể thay vì từng trạng thái riêng lẻ
        if (assignedStaff != null && !assignedStaff.trim().isEmpty()) {
            try {
                staffWorkloadService.validateCanAcceptNewTask(assignedStaff.trim());
                
                // Log workload info để monitoring
                StaffWorkloadService.WorkloadInfo workloadInfo = 
                        staffWorkloadService.getWorkloadInfo(assignedStaff.trim());
                log.info("Staff '{}' workload before assignment: {} active videos (DANG_LAM: {}, DANG_SUA: {}, CAN_SUA_GAP: {})",
                        assignedStaff.trim(), workloadInfo.getTotalActive(), 
                        workloadInfo.getDangLamCount(), workloadInfo.getDangSuaCount(), 
                        workloadInfo.getCanSuaGapCount());
                        
            } catch (IllegalArgumentException e) {
                // Re-throw với context thêm về video đang cố gắng assign
                log.warn("Cannot assign video ID {} to staff '{}': {}", id, assignedStaff.trim(), e.getMessage());
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        // Lưu giá trị cũ để audit logging
        String oldAssignedStaff = existingVideo.getAssignedStaff();
        VideoStatus oldStatus = existingVideo.getStatus();
        
        existingVideo.setAssignedStaff(assignedStaff != null ? assignedStaff.trim() : null);
        
        // Tự động chuyển trạng thái sang DANG_LAM khi gán nhân viên và set thời gian assign
        if (assignedStaff != null && !assignedStaff.trim().isEmpty()) {
            existingVideo.setStatus(VideoStatus.DANG_LAM);
            existingVideo.setAssignedAt(LocalDateTime.now()); // Lưu thời gian assign
        } else {
            // Nếu không có nhân viên được gán thì chuyển về CHUA_AI_NHAN và clear assignedAt
            existingVideo.setStatus(VideoStatus.CHUA_AI_NHAN);
            existingVideo.setAssignedAt(null);
        }
        
        Video updatedVideo = videoRepository.save(existingVideo);

        // Log successful assignment với workload info
        if (assignedStaff != null && !assignedStaff.trim().isEmpty()) {
            long newWorkload = staffWorkloadService.getCurrentWorkload(assignedStaff.trim());
            log.info("Video ID {} successfully assigned to staff '{}'. New workload: {} videos", 
                    id, assignedStaff.trim(), newWorkload);
            
            // Audit log cho việc gán nhân viên
            auditService.logVideoBusinessAction(
                updatedVideo.getId(),
                AuditAction.ASSIGN_STAFF,
                String.format("Gán video cho nhân viên: %s", assignedStaff.trim()),
                "assignedStaff",
                oldAssignedStaff,
                assignedStaff.trim()
            );
            
            // Audit log cho việc thay đổi trạng thái
            if (oldStatus != updatedVideo.getStatus()) {
                auditService.logVideoBusinessAction(
                    updatedVideo.getId(),
                    AuditAction.UPDATE_STATUS,
                    String.format("Tự động chuyển trạng thái từ '%s' sang '%s' khi gán nhân viên", oldStatus, updatedVideo.getStatus()),
                    "status",
                    oldStatus.name(),
                    updatedVideo.getStatus().name()
                );
            }
        } else {
            log.info("Video ID {} unassigned from staff", id);
            
            // Audit log cho việc hủy gán nhân viên
            auditService.logVideoBusinessAction(
                updatedVideo.getId(),
                AuditAction.UNASSIGN_STAFF,
                "Hủy gán nhân viên khỏi video",
                "assignedStaff",
                oldAssignedStaff,
                null
            );
            
            // Audit log cho việc thay đổi trạng thái
            if (oldStatus != updatedVideo.getStatus()) {
                auditService.logVideoBusinessAction(
                    updatedVideo.getId(),
                    AuditAction.UPDATE_STATUS,
                    String.format("Tự động chuyển trạng thái từ '%s' sang '%s' khi hủy gán nhân viên", oldStatus, updatedVideo.getStatus()),
                    "status",
                    oldStatus.name(),
                    updatedVideo.getStatus().name()
                );
            }
        }

        return mapToResponseDto(updatedVideo);
    }
    /**
     * Cập nhật trạng thái video
     * 
     * PERMISSION LOGIC: Chỉ có nhân viên được giao video mới có quyền cập nhật trạng thái
     * Kiểm tra bằng cách so sánh trường "name" từ JWT với assignedStaff của video
     */
    @Transactional
    public VideoResponseDto updateVideoStatus(Long id, String statusString) {
        log.info("Updating status for video ID: {} to status: {}", id, statusString);

        Video existingVideo = findVideoByIdOrThrow(id);
        
        // SECURITY CHECK: Kiểm tra quyền cập nhật video
        if (!jwtTokenService.hasPermissionToUpdateVideo(existingVideo.getAssignedStaff())) {
            String currentUser = jwtTokenService.getCurrentUserNameFromJwt();
            log.warn("User '{}' attempted to update video ID {} but is not authorized. Assigned staff: '{}'", 
                    currentUser, id, existingVideo.getAssignedStaff());
            throw new SecurityException("Bạn không có quyền cập nhật trạng thái video này. " +
                    "Chỉ có nhân viên được giao video mới có thể cập nhật.");
        }

        // Validate và convert status string to enum
        if (!StringUtils.hasText(statusString)) {
            throw new IllegalArgumentException("Trạng thái không được để trống");
        }

        VideoStatus status;
        try {
            status = VideoStatus.valueOf(statusString.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid video status: '{}'", statusString);
            throw new IllegalArgumentException("Trạng thái video không hợp lệ: " + statusString +
                    ". Các trạng thái hợp lệ: CHUA_AI_NHAN, DANG_LAM, DA_XONG, DANG_SUA, DA_SUA_XONG");
        }

        // Kiểm tra nếu đang cố gắng chuyển sang CHUA_AI_NHAN khi video đã có người nhận
        if (status == VideoStatus.CHUA_AI_NHAN && 
            existingVideo.getAssignedStaff() != null && 
            !existingVideo.getAssignedStaff().trim().isEmpty() &&
            (existingVideo.getStatus() == VideoStatus.DANG_LAM ||
             existingVideo.getStatus() == VideoStatus.DANG_SUA ||
             existingVideo.getStatus() == VideoStatus.DA_XONG ||
             existingVideo.getStatus() == VideoStatus.DA_SUA_XONG)) {
            throw new IllegalArgumentException("Hiện tại video này đã có người nhận, không thể cập nhật trạng thái sang chưa ai nhận");
        }

        // Check if video URL exists when changing status to DA_XONG or DA_SUA_XONG
        if ((status == VideoStatus.DA_XONG || status == VideoStatus.DA_SUA_XONG) 
            && (existingVideo.getVideoUrl() == null || existingVideo.getVideoUrl().trim().isEmpty())) {
            throw new IllegalArgumentException("Phải có link video trước khi chuyển sang xong hoặc đã xong");
        }

        // Lưu trạng thái cũ để audit logging
        VideoStatus oldStatus = existingVideo.getStatus();
        
        existingVideo.setStatus(status);

        // Tự động set completedTime khi status = DA_XONG hoặc DA_SUA_XONG
        if ((status == VideoStatus.DA_XONG || status == VideoStatus.DA_SUA_XONG)
                && existingVideo.getCompletedTime() == null) {
            existingVideo.setCompletedTime(LocalDateTime.now());
        }

        Video updatedVideo = videoRepository.save(existingVideo);

        // Audit log cho việc thay đổi trạng thái
        if (oldStatus != status) {
            auditService.logVideoBusinessAction(
                updatedVideo.getId(),
                AuditAction.UPDATE_STATUS,
                String.format("Thay đổi trạng thái video từ '%s' sang '%s'", oldStatus, status),
                "status",
                oldStatus.name(),
                status.name()
            );
        }

        log.info("Video status updated successfully for video ID: {} to status: {} by user: {}", 
                id, status, jwtTokenService.getCurrentUserNameFromJwt());
        return mapToResponseDto(updatedVideo);
    }
    /**
     * Cập nhật link video
     * 
     * PERMISSION LOGIC: Chỉ có nhân viên được giao video mới có quyền cập nhật video URL
     */
    @Transactional
    public VideoResponseDto updateVideoUrl(Long id, String videoUrl) {
        log.info("Updating video URL for video ID: {} to URL: {}", id, videoUrl);

        Video existingVideo = findVideoByIdOrThrow(id);
        
        // SECURITY CHECK: Kiểm tra quyền cập nhật video
        if (!jwtTokenService.hasPermissionToUpdateVideo(existingVideo.getAssignedStaff())) {
            String currentUser = jwtTokenService.getCurrentUserNameFromJwt();
            log.warn("User '{}' attempted to update video URL for video ID {} but is not authorized. Assigned staff: '{}'", 
                    currentUser, id, existingVideo.getAssignedStaff());
            throw new SecurityException("Bạn không có quyền cập nhật link video này. " +
                    "Chỉ có nhân viên được giao video mới có thể cập nhật.");
        }

        // Validate video URL
        if (videoUrl != null && videoUrl.length() > 500) {
            throw new IllegalArgumentException("URL video không được vượt quá 500 ký tự");
        }

        // Lưu giá trị cũ để audit logging
        String oldVideoUrl = existingVideo.getVideoUrl();
        
        existingVideo.setVideoUrl(videoUrl != null ? videoUrl.trim() : null);
        Video updatedVideo = videoRepository.save(existingVideo);
        
        // Audit log cho việc cập nhật link video
        auditService.logVideoBusinessAction(
            updatedVideo.getId(),
            AuditAction.UPDATE_VIDEO_URL,
            "Cập nhật link video",
            "videoUrl",
            oldVideoUrl,
            videoUrl != null ? videoUrl.trim() : null
        );

        log.info("Video URL updated successfully for video ID: {} by user: {}", 
                id, jwtTokenService.getCurrentUserNameFromJwt());
        return mapToResponseDto(updatedVideo);
    }

    /**
     * Hủy video - Reset về trạng thái chưa ai nhận (ADMIN ONLY)
     */
    @Transactional
    public VideoResponseDto cancelVideo(Long id) {
        log.info("Admin canceling video with ID: {}", id);

        // SECURITY CHECK: Chỉ admin mới có quyền hủy video
        if (!jwtTokenService.isCurrentUserAdmin()) {
            String currentUser = jwtTokenService.getCurrentUserNameFromJwt();
            log.warn("User '{}' attempted to cancel video ID {} but is not admin", currentUser, id);
            throw new SecurityException("Chỉ có admin mới có quyền hủy video");
        }

        Video existingVideo = findVideoByIdOrThrow(id);
        
        // Log trước khi hủy để audit trail
        log.info("Canceling video ID: {}, current assigned staff: '{}', status: {}", 
                id, existingVideo.getAssignedStaff(), existingVideo.getStatus());

        // Lưu giá trị cũ để audit logging
        String oldAssignedStaff = existingVideo.getAssignedStaff();
        VideoStatus oldStatus = existingVideo.getStatus();
        
        // Reset video về trạng thái ban đầu
        existingVideo.setAssignedStaff(null);
        existingVideo.setAssignedAt(null);
        existingVideo.setStatus(VideoStatus.CHUA_AI_NHAN);

        Video canceledVideo = videoRepository.save(existingVideo);
        
        // Audit log cho việc hủy video
        auditService.logVideoBusinessAction(
            canceledVideo.getId(),
            AuditAction.CANCEL,
            String.format("Admin hủy video - Reset từ nhân viên '%s' và trạng thái '%s'", oldAssignedStaff, oldStatus),
            null,
            null,
            null
        );

        String adminUser = jwtTokenService.getCurrentUserNameFromJwt();
        log.info("Video ID: {} successfully canceled by admin: '{}'", id, adminUser);
        
        return mapToResponseDto(canceledVideo);
    }
    /**
     * Xóa video (soft delete)
     */
    @Transactional
    public void deleteVideo(Long id) {
        log.info("Deleting video with ID: {}", id);

        Video video = videoRepository.findById(id).orElseThrow(
                () -> new VideoNotFoundException("Không tìm thấy video với ID: " + id)
        );

        video.setIsDeleted(true);
        videoRepository.save(video);
        
        // Audit log cho việc xóa video
        auditService.logVideoBusinessAction(
            video.getId(),
            AuditAction.DELETE,
            String.format("Xóa video của khách hàng: %s", video.getCustomerName()),
            null,
            null,
            null
        );
        
        log.info("Video deleted successfully with ID: {}", id);
    }

    /**
     * Lấy thông tin chi tiết một video
     */
    public VideoResponseDto getVideoById(Long id) {
        log.info("Fetching video with ID: {}", id);
        Video video = findVideoByIdOrThrow(id);
        return mapToResponseDto(video);
    }

    /**
     * Lấy danh sách tất cả video với phân trang
     */
    public Page<VideoResponseDto> getAllVideos(int page, int size, String sortBy, String sortDirection,
                                               VideoStatus videoStatus, String assignedStaff, 
                                               DeliveryStatus deliveryStatus, PaymentStatus paymentStatus,
                                               LocalDate paymentDate, String createdBy) {
        log.info("Fetching all videos - page: {}, size: {}, sortBy: {}, direction: {}",
                page, size, sortBy, sortDirection);

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Video> videoPage = videoRepository.getAll(pageable, videoStatus, assignedStaff, deliveryStatus, paymentStatus, paymentDate, createdBy);
        return videoPage.map(this::mapToResponseDto);
    }

    /**
     * Lấy danh sách video không phân trang
     */
    public List<VideoResponseDto> getAllVideos() {
        log.info("Fetching all videos without pagination");
        List<Video> videos = videoRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return videos.stream().map(this::mapToResponseDto).toList();
    }

    /**
     * Tìm kiếm video theo tên khách hàng
     */
    public List<VideoResponseDto> searchByCustomerName(String customerName) {
        log.info("Searching videos by customer name: '{}'", customerName);

        if (!StringUtils.hasText(customerName)) {
            log.warn("Customer name is empty or null, returning empty list");
            return List.of();
        }

        String trimmedName = customerName.trim();
        if (trimmedName.length() < 2) {
            log.warn("Customer name too short (less than 2 characters): '{}'", trimmedName);
            throw new IllegalArgumentException("Tên khách hàng phải có ít nhất 2 ký tự");
        }

        List<Video> videos = videoRepository.findByCustomerNameContainingIgnoreCase(trimmedName);
        log.info("Found {} videos for customer name: '{}'", videos.size(), trimmedName);
        return videos.stream().map(this::mapToResponseDto).toList();
    }
    /**
     * Lấy danh sách video theo trạng thái
     */
    public List<VideoResponseDto> getVideosByStatus(String statusString) {
        log.info("Fetching videos by status: '{}'", statusString);

        if (!StringUtils.hasText(statusString)) {
            throw new IllegalArgumentException("Trạng thái không được để trống");
        }

        VideoStatus status;
        try {
            status = VideoStatus.valueOf(statusString.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid video status: '{}'", statusString);
            throw new IllegalArgumentException("Trạng thái video không hợp lệ: " + statusString +
                    ". Các trạng thái hợp lệ: CHUA_AI_NHAN, DANG_LAM, DA_XONG, DANG_SUA, DA_SUA_XONG");
        }

        List<Video> videos = videoRepository.findByStatus(status);
        log.info("Found {} videos with status: '{}'", videos.size(), status);
        return videos.stream().map(this::mapToResponseDto).toList();
    }

    /**
     * Tìm kiếm video theo nhân viên được giao
     */
    public List<VideoResponseDto> searchByAssignedStaff(String assignedStaff) {
        log.info("Searching videos by assigned staff: '{}'", assignedStaff);

        if (!StringUtils.hasText(assignedStaff)) {
            log.warn("Assigned staff is empty or null, returning empty list");
            return List.of();
        }

        String trimmedStaff = assignedStaff.trim();
        if (trimmedStaff.length() < 2) {
            log.warn("Assigned staff name too short (less than 2 characters): '{}'", trimmedStaff);
            throw new IllegalArgumentException("Tên nhân viên phải có ít nhất 2 ký tự");
        }

        List<Video> videos = videoRepository.findByAssignedStaffContainingIgnoreCase(trimmedStaff);
        log.info("Found {} videos assigned to staff: '{}'", videos.size(), trimmedStaff);
        return videos.stream().map(this::mapToResponseDto).toList();
    }

    /**
     * Lấy thống kê số lượng video theo từng trạng thái
     */
    public Map<VideoStatus, Long> getVideoStatusStatistics() {
        log.info("Fetching video status statistics");
        Map<VideoStatus, Long> statistics = new HashMap<>();

        for (VideoStatus status : VideoStatus.values()) {
            long count = videoRepository.countByStatus(status);
            statistics.put(status, count);
            log.debug("Status {}: {} videos", status, count);
        }

        log.info("Video status statistics retrieved successfully");
        return statistics;
    }
    /**
     * Tìm kiếm video trong khoảng thời gian tạo
     */
    public Page<VideoResponseDto> getVideosByDateRange(LocalDateTime startDate, LocalDateTime endDate,
                                                       int page, int size) {
        log.info("Fetching videos created between {} and {}", startDate, endDate);

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc không được để trống");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Video> videoPage = videoRepository.findByCreatedAtBetween(startDate, endDate, pageable);
        log.info("Found {} videos created between {} and {}", videoPage.getTotalElements(), startDate, endDate);
        return videoPage.map(this::mapToResponseDto);
    }

    /**
     * Tìm kiếm video nâng cao với nhiều điều kiện
     */
    public List<VideoResponseDto> advancedSearch(String customerName, VideoStatus status,
                                                 String assignedStaff, LocalDateTime startDate,
                                                 LocalDateTime endDate) {
        log.info("Performing advanced search with filters - customer: '{}', status: '{}', staff: '{}', date range: {} to {}",
                customerName, status, assignedStaff, startDate, endDate);

        List<Video> videos = videoRepository.findAll();

        if (StringUtils.hasText(customerName)) {
            videos = videos.stream()
                    .filter(video -> video.getCustomerName() != null &&
                            video.getCustomerName().toLowerCase().contains(customerName.toLowerCase()))
                    .toList();
        }

        if (status != null) {
            videos = videos.stream()
                    .filter(video -> video.getStatus() == status)
                    .toList();
        }

        if (StringUtils.hasText(assignedStaff)) {
            videos = videos.stream()
                    .filter(video -> video.getAssignedStaff() != null &&
                            video.getAssignedStaff().toLowerCase().contains(assignedStaff.toLowerCase()))
                    .toList();
        }

        if (startDate != null && endDate != null) {
            videos = videos.stream()
                    .filter(video -> video.getCreatedAt() != null &&
                            !video.getCreatedAt().isBefore(startDate) &&
                            !video.getCreatedAt().isAfter(endDate))
                    .toList();
        }

        log.info("Advanced search returned {} videos", videos.size());
        return videos.stream().map(this::mapToResponseDto).toList();
    }
    /**
     * Lọc video theo nhân viên, trạng thái giao hàng và trạng thái thanh toán
     */
    public List<VideoResponseDto> filterVideos(String assignedStaff, String deliveryStatus, String paymentStatus) {
        log.info("Filtering videos - staff: '{}', delivery status: '{}', payment status: '{}'",
                assignedStaff, deliveryStatus, paymentStatus);

        List<Video> videos = videoRepository.findAll();

        // Lọc theo nhân viên được giao
        if (StringUtils.hasText(assignedStaff)) {
            String trimmedStaff = assignedStaff.trim();
            if (trimmedStaff.length() < 2) {
                throw new IllegalArgumentException("Tên nhân viên phải có ít nhất 2 ký tự");
            }
            videos = videos.stream()
                    .filter(video -> video.getAssignedStaff() != null &&
                            video.getAssignedStaff().toLowerCase().contains(trimmedStaff.toLowerCase()))
                    .toList();
        }

        // Lọc theo trạng thái giao hàng
        if (StringUtils.hasText(deliveryStatus)) {
            try {
                DeliveryStatus status = DeliveryStatus.valueOf(deliveryStatus.toUpperCase());
                videos = videos.stream()
                        .filter(video -> video.getDeliveryStatus() == status)
                        .toList();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Trạng thái giao hàng không hợp lệ: " + deliveryStatus +
                        ". Các trạng thái hợp lệ: CHUA_GUI, DANG_GUI, DA_GUI");
            }
        }

        // Lọc theo trạng thái thanh toán
        if (StringUtils.hasText(paymentStatus)) {
            try {
                PaymentStatus status = PaymentStatus.valueOf(paymentStatus.toUpperCase());
                videos = videos.stream()
                        .filter(video -> video.getPaymentStatus() == status)
                        .toList();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Trạng thái thanh toán không hợp lệ: " + paymentStatus +
                        ". Các trạng thái hợp lệ: CHUA_THANH_TOAN, DA_THANH_TOAN");
            }
        }

        log.info("Filter returned {} videos", videos.size());
        return videos.stream().map(this::mapToResponseDto).toList();
    }

    /**
     * Cập nhật trạng thái giao hàng
     */
    @Transactional
    public VideoResponseDto updateDeliveryStatus(Long id, String statusString) {
        log.info("Updating delivery status for video ID: {} to status: {}", id, statusString);

        Video existingVideo = findVideoByIdOrThrow(id);

        // Validate và convert status string to enum
        if (!StringUtils.hasText(statusString)) {
            throw new IllegalArgumentException("Trạng thái giao hàng không được để trống");
        }

        DeliveryStatus status;
        try {
            status = DeliveryStatus.valueOf(statusString.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid delivery status: '{}'", statusString);
            throw new IllegalArgumentException("Trạng thái giao hàng không hợp lệ: " + statusString +
                    ".Các trạng thái hợp lệ: CHUA_GUI, DANG_GUI, DA_GUI");
        }

        // Lưu giá trị cũ để audit logging
        DeliveryStatus oldDeliveryStatus = existingVideo.getDeliveryStatus();
        
        existingVideo.setDeliveryStatus(status);
        Video updatedVideo = videoRepository.save(existingVideo);
        
        // Audit log cho việc thay đổi trạng thái giao hàng
        if (oldDeliveryStatus != status) {
            auditService.logVideoBusinessAction(
                updatedVideo.getId(),
                AuditAction.UPDATE_DELIVERY_STATUS,
                String.format("Thay đổi trạng thái giao hàng từ '%s' sang '%s'", 
                    oldDeliveryStatus != null ? oldDeliveryStatus : "CHUA_GUI", status),
                "deliveryStatus",
                oldDeliveryStatus != null ? oldDeliveryStatus.name() : null,
                status.name()
            );
        }

        log.info("Delivery status updated successfully for video ID: {} to status: {}", id, status);
        return mapToResponseDto(updatedVideo);
    }
    /**
     * Cập nhật trạng thái thanh toán
     */
    @Transactional
    public VideoResponseDto updatePaymentStatus(Long id, String statusString) {
        log.info("Updating payment status for video ID: {} to status: {}", id, statusString);

        Video existingVideo = findVideoByIdOrThrow(id);

        // Validate và convert status string to enum
        if (!StringUtils.hasText(statusString)) {
            throw new IllegalArgumentException("Trạng thái thanh toán không được để trống");
        }

        PaymentStatus status;
        try {
            status = PaymentStatus.valueOf(statusString.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid payment status: '{}'", statusString);
            throw new IllegalArgumentException("Trạng thái thanh toán không hợp lệ: " + statusString +
                    ". Các trạng thái hợp lệ: CHUA_THANH_TOAN, DA_THANH_TOAN");
        }

        // Lưu giá trị cũ để audit logging
        PaymentStatus oldPaymentStatus = existingVideo.getPaymentStatus();
        
        existingVideo.setPaymentStatus(status);

        // Tự động set paymentDate khi status = DA_THANH_TOAN
        if (status == PaymentStatus.DA_THANH_TOAN && existingVideo.getPaymentDate() == null) {
            existingVideo.setPaymentDate(LocalDateTime.now());
        }

        Video updatedVideo = videoRepository.save(existingVideo);
        
        // Audit log cho việc thay đổi trạng thái thanh toán
        if (oldPaymentStatus != status) {
            auditService.logVideoBusinessAction(
                updatedVideo.getId(),
                AuditAction.UPDATE_PAYMENT_STATUS,
                String.format("Thay đổi trạng thái thanh toán từ '%s' sang '%s'", 
                    oldPaymentStatus != null ? oldPaymentStatus : "CHUA_THANH_TOAN", status),
                "paymentStatus",
                oldPaymentStatus != null ? oldPaymentStatus.name() : null,
                status.name()
            );
        }

        log.info("Payment status updated successfully for video ID: {} to status: {}", id, status);
        return mapToResponseDto(updatedVideo);
    }

    /**
     * Lấy danh sách các nhân viên được giao khác nhau
     */
    public List<String> getDistinctAssignedStaff() {
        log.info("Fetching distinct assigned staff names");
        List<String> staffList = videoRepository.findDistinctAssignedStaff();
        log.info("Found {} distinct staff members", staffList.size());
        return staffList;
    }

    /**
     * Lấy danh sách các người tạo video khác nhau
     */
    public List<String> getDistinctCreatedBy() {
        log.info("Fetching distinct video creators");
        List<String> creatorList = videoRepository.findDistinctCreatedBy();
        log.info("Found {} distinct video creators", creatorList.size());
        return creatorList;
    }

    /**
     * Tính tổng tiền lương cho các nhân viên
     */
    public List<StaffSalaryDto> calculateStaffSalaries(LocalDate date) {
        log.info("Calculating staff salaries for date: {}", date);
        List<StaffSalaryDto> salaries = videoRepository.calculateStaffSalaries(date);
        log.info("Found salary information for {} staff members", salaries.size());
        return salaries;
    }

    /**
     * Kiểm tra khách hàng đã tồn tại trong hệ thống hay chưa
     * 
     * Business Logic:
     * - Tìm kiếm khách hàng theo tên (case-insensitive, exact match)
     * - Chỉ kiểm tra các video chưa bị xóa (isDeleted = false)
     * - Trả về true nếu tìm thấy ít nhất 1 video của khách hàng
     * 
     * @param customerName Tên khách hàng cần kiểm tra
     * @return true nếu khách hàng đã tồn tại, false nếu chưa tồn tại
     * @throws IllegalArgumentException nếu tên khách hàng không hợp lệ
     */
    public boolean checkCustomerExists(String customerName) {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên khách hàng không được để trống");
        }

        String trimmedName = customerName.trim();
        if (trimmedName.length() < 2) {
            throw new IllegalArgumentException("Tên khách hàng phải có ít nhất 2 ký tự");
        }

        log.info("Checking if customer exists: '{}'", trimmedName);
        
        // Sử dụng method có sẵn để tìm kiếm khách hàng
        List<Video> existingVideos = videoRepository.findByCustomerNameContainingIgnoreCase(trimmedName);
        
        // Lọc ra những video chưa bị xóa và có tên khách hàng chính xác (exact match, case-insensitive)
        boolean exists = existingVideos.stream()
                .filter(video -> !video.getIsDeleted()) // Chỉ kiểm tra video chưa bị xóa
                .anyMatch(video -> video.getCustomerName() != null && 
                         video.getCustomerName().trim().equalsIgnoreCase(trimmedName));

        log.info("Customer '{}' existence check result: {}", trimmedName, exists);
        
        if (exists) {
            log.info("Found existing customer '{}' in system - potential duplicate order warning", trimmedName);
        }
        
        return exists;
    }
    /**
     * Tính lương sales theo ngày thanh toán - UPDATED với Interface Projection
     * 
     * FIXED: Thay đổi từ constructor expression sang interface projection
     * để tránh lỗi "Missing constructor" trong JPQL
     */
    public List<SalesSalaryDto> calculateSalesSalariesByDate(LocalDate targetDate) {
        if (targetDate == null) {
            throw new IllegalArgumentException("Target date không được null");
        }
        
        log.info("Calculating sales salaries for date: {}", targetDate);
        
        List<SalesSalaryDto> salesSalaries;
        
        try {
            // Thử sử dụng interface projection trước
            log.debug("Attempting to use interface projection for sales salary calculation");
            List<SalesSalaryProjection> projections = videoRepository.calculateSalesSalariesProjectionByDate(targetDate);
            
            // Convert projection sang DTO với đầy đủ thông tin
            salesSalaries = projections.stream()
                    .map(projection -> convertProjectionToDto(projection, targetDate))
                    .toList();
                    
            log.info("Successfully used interface projection for sales salary calculation");
            
        } catch (Exception e) {
            log.warn("Interface projection failed, falling back to native query: {}", e.getMessage());
            
            // Fallback sang native query nếu projection fail
            salesSalaries = calculateSalesSalariesByDateNative(targetDate);
        }
        
        // Log chi tiết để debugging và monitoring
        logSalaryCalculationResults(salesSalaries, targetDate);
        
        return salesSalaries;
    }
    
    /**
     * Helper method để log kết quả tính lương sales
     */
    private void logSalaryCalculationResults(List<SalesSalaryDto> salesSalaries, LocalDate targetDate) {
        if (salesSalaries.isEmpty()) {
            log.warn("No sales salary data found for date: {}", targetDate);
        } else {
            BigDecimal totalCommission = salesSalaries.stream()
                    .map(SalesSalaryDto::getCommissionSalary)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Long totalVideos = salesSalaries.stream()
                    .mapToLong(SalesSalaryDto::getTotalPaidVideos)
                    .sum();
            
            log.info("Sales salary calculation completed for date: {} - {} sales, {} videos, total commission: {}", 
                    targetDate, salesSalaries.size(), totalVideos, totalCommission);
        }
    }
    /**
     * Helper method để convert SalesSalaryProjection sang SalesSalaryDto
     */
    private SalesSalaryDto convertProjectionToDto(SalesSalaryProjection projection, LocalDate targetDate) {
        return SalesSalaryDto.builder()
                .salesName(projection.getSalesName())
                .salaryDate(targetDate.toString()) // Convert LocalDate to String
                .totalPaidVideos(projection.getTotalPaidVideos())
                .totalSalesValue(projection.getTotalSalesValue())
                .commissionSalary(projection.getCommissionSalary())
                .commissionRate(BigDecimal.valueOf(0.12)) // 12% commission rate
                .build();
    }

    /**
     * BACKUP: Method tính lương sales sử dụng native query
     */
    private List<SalesSalaryDto> calculateSalesSalariesByDateNative(LocalDate targetDate) {
        log.info("Using native query fallback for sales salaries calculation: {}", targetDate);
        
        List<Object[]> results = videoRepository.calculateSalesSalariesNativeByDate(targetDate);
        
        return results.stream()
                .map(row -> convertNativeResultToDto(row, targetDate))
                .toList();
    }

    /**
     * Helper method để convert native query result sang DTO
     */
    private SalesSalaryDto convertNativeResultToDto(Object[] row, LocalDate targetDate) {
        return SalesSalaryDto.builder()
                .salesName((String) row[0])
                .salaryDate(targetDate.toString())
                .totalPaidVideos(((Number) row[1]).longValue())
                .totalSalesValue((BigDecimal) row[2])
                .commissionSalary((BigDecimal) row[3])
                .commissionRate(BigDecimal.valueOf(0.12))
                .build();
    }

    /**
     * Helper method để tìm video theo ID, throw exception nếu không tìm thấy
     */
    private Video findVideoByIdOrThrow(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new VideoNotFoundException("Không tìm thấy video với ID: " + id));
    }
    /**
     * Helper method để update các field của video entity
     */
    private void updateVideoFields(Video existingVideo, VideoRequestDto requestDto) {
        Optional.ofNullable(requestDto.getCustomerName())
                .ifPresent(existingVideo::setCustomerName);
        Optional.ofNullable(requestDto.getVideoContent())
                .ifPresent(existingVideo::setVideoContent);
        Optional.ofNullable(requestDto.getImageUrl())
                .ifPresent(existingVideo::setImageUrl);
        Optional.ofNullable(requestDto.getVideoDuration())
                .ifPresent(existingVideo::setVideoDuration);
        Optional.ofNullable(requestDto.getDeliveryTime())
                .ifPresent(existingVideo::setDeliveryTime);
        Optional.ofNullable(requestDto.getAssignedStaff())
                .ifPresent(existingVideo::setAssignedStaff);
        Optional.ofNullable(requestDto.getStatus())
                .ifPresent(existingVideo::setStatus);
        Optional.ofNullable(requestDto.getVideoUrl())
                .ifPresent(existingVideo::setVideoUrl);
        Optional.ofNullable(requestDto.getCompletedTime())
                .ifPresent(existingVideo::setCompletedTime);
        Optional.ofNullable(requestDto.getCustomerApproved())
                .ifPresent(existingVideo::setCustomerApproved);
        Optional.ofNullable(requestDto.getCustomerNote())
                .ifPresent(existingVideo::setCustomerNote);
        Optional.ofNullable(requestDto.getChecked())
                .ifPresent(existingVideo::setChecked);
        Optional.ofNullable(requestDto.getDeliveryStatus())
                .ifPresent(existingVideo::setDeliveryStatus);
        Optional.ofNullable(requestDto.getPaymentStatus())
                .ifPresent(status -> {
                    existingVideo.setPaymentStatus(status);
                    if (status == PaymentStatus.DA_THANH_TOAN && existingVideo.getPaymentDate() == null) {
                        existingVideo.setPaymentDate(LocalDateTime.now());
                    }
                });
        Optional.ofNullable(requestDto.getOrderValue())
                .ifPresent(existingVideo::setOrderValue);
        Optional.ofNullable(requestDto.getPrice())
                .ifPresent(existingVideo::setPrice);
    }
    /**
     * Map từ DTO request sang Entity
     */
    private Video mapToEntity(VideoRequestDto dto) {
        return Video.builder()
                .customerName(dto.getCustomerName())
                .videoContent(dto.getVideoContent())
                .imageUrl(dto.getImageUrl())
                .videoDuration(dto.getVideoDuration())
                .deliveryTime(dto.getDeliveryTime())
                .assignedStaff(dto.getAssignedStaff())
                .status(dto.getStatus())
                .videoUrl(dto.getVideoUrl())
                .completedTime(dto.getCompletedTime())
                .customerApproved(dto.getCustomerApproved())
                .customerNote(dto.getCustomerNote())
                .checked(dto.getChecked())
                .deliveryStatus(dto.getDeliveryStatus())
                .paymentStatus(dto.getPaymentStatus())
                .paymentDate(dto.getPaymentDate())
                .orderValue(dto.getOrderValue())
                .price(dto.getPrice())
                .build();
    }

    /**
     * Map từ Entity sang DTO response
     */
    private VideoResponseDto mapToResponseDto(Video video) {
        return VideoResponseDto.builder()
                .id(video.getId())
                .createdBy(video.getCreatedBy())
                .customerName(video.getCustomerName())
                .videoContent(video.getVideoContent())
                .imageUrl(video.getImageUrl())
                .videoDuration(video.getVideoDuration())
                .deliveryTime(video.getDeliveryTime())
                .assignedStaff(video.getAssignedStaff())
                .assignedAt(video.getAssignedAt())
                .status(video.getStatus())
                .videoUrl(video.getVideoUrl())
                .completedTime(video.getCompletedTime())
                .customerApproved(video.getCustomerApproved())
                .customerNote(video.getCustomerNote())
                .checked(video.getChecked())
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .deliveryStatus(video.getDeliveryStatus())
                .paymentStatus(video.getPaymentStatus())
                .paymentDate(video.getPaymentDate())
                .orderValue(video.getOrderValue())
                .build();
    }

    /**
     * Custom exception cho trường hợp không tìm thấy video
     */
    public static class VideoNotFoundException extends RuntimeException {
        public VideoNotFoundException(String message) {
            super(message);
        }
    }
}