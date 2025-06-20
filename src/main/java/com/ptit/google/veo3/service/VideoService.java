package com.ptit.google.veo3.service;

import com.ptit.google.veo3.dto.StaffSalaryDto;
import com.ptit.google.veo3.dto.VideoRequestDto;
import com.ptit.google.veo3.dto.VideoResponseDto;
import com.ptit.google.veo3.entity.DeliveryStatus;
import com.ptit.google.veo3.entity.PaymentStatus;
import com.ptit.google.veo3.entity.Video;
import com.ptit.google.veo3.entity.VideoStatus;
import com.ptit.google.veo3.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    /**
     * Tạo mới một video record
     */
    @Transactional
    public VideoResponseDto createVideo(VideoRequestDto requestDto) {
        log.info("Creating new video for customer: {}", requestDto.getCustomerName());

        Video video = mapToEntity(requestDto);
        Video savedVideo = videoRepository.save(video);

        log.info("Video created successfully with ID: {}", savedVideo.getId());
        return mapToResponseDto(savedVideo);
    }

    /**
     * Cập nhật thông tin video
     */
    @Transactional
    public VideoResponseDto updateVideo(Long id, VideoRequestDto requestDto) {
        log.info("Updating video with ID: {}", id);

        Video existingVideo = findVideoByIdOrThrow(id);
        updateVideoFields(existingVideo, requestDto);

        Video updatedVideo = videoRepository.save(existingVideo);

        log.info("Video updated successfully with ID: {}", id);
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

        existingVideo.setAssignedStaff(assignedStaff != null ? assignedStaff.trim() : null);
        
        // Tự động chuyển trạng thái sang DANG_LAM khi gán nhân viên
        if (assignedStaff != null && !assignedStaff.trim().isEmpty()) {
            existingVideo.setStatus(VideoStatus.DANG_LAM);
        } else {
            // Nếu không có nhân viên được gán thì chuyển về CHUA_AI_NHAN
            existingVideo.setStatus(VideoStatus.CHUA_AI_NHAN);
        }
        
        Video updatedVideo = videoRepository.save(existingVideo);

        // Log successful assignment với workload info
        if (assignedStaff != null && !assignedStaff.trim().isEmpty()) {
            long newWorkload = staffWorkloadService.getCurrentWorkload(assignedStaff.trim());
            log.info("Video ID {} successfully assigned to staff '{}'. New workload: {} videos", 
                    id, assignedStaff.trim(), newWorkload);
        } else {
            log.info("Video ID {} unassigned from staff", id);
        }

        return mapToResponseDto(updatedVideo);
    }

    /**
     * Cập nhật trạng thái video
     * 
     * PERMISSION LOGIC: Chỉ có nhân viên được giao video mới có quyền cập nhật trạng thái
     * Kiểm tra bằng cách so sánh trường "name" từ JWT với assignedStaff của video
     * 
     * @param id Video ID cần cập nhật
     * @param statusString Trạng thái mới 
     * @return VideoResponseDto sau khi cập nhật
     * @throws SecurityException nếu người dùng không có quyền
     * @throws VideoNotFoundException nếu không tìm thấy video
     * @throws IllegalArgumentException nếu tham số không hợp lệ
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

        existingVideo.setStatus(status);

        // Tự động set completedTime khi status = DA_XONG hoặc DA_SUA_XONG
        if ((status == VideoStatus.DA_XONG || status == VideoStatus.DA_SUA_XONG)
                && existingVideo.getCompletedTime() == null) {
            existingVideo.setCompletedTime(LocalDateTime.now());
        }

        Video updatedVideo = videoRepository.save(existingVideo);

        log.info("Video status updated successfully for video ID: {} to status: {} by user: {}", 
                id, status, jwtTokenService.getCurrentUserNameFromJwt());
        return mapToResponseDto(updatedVideo);
    }

    /**
     * Cập nhật link video
     * 
     * PERMISSION LOGIC: Chỉ có nhân viên được giao video mới có quyền cập nhật video URL
     * Kiểm tra bằng cách so sánh trường "name" từ JWT với assignedStaff của video
     * 
     * @param id Video ID cần cập nhật
     * @param videoUrl Link video mới
     * @return VideoResponseDto sau khi cập nhật
     * @throws SecurityException nếu người dùng không có quyền
     * @throws VideoNotFoundException nếu không tìm thấy video
     * @throws IllegalArgumentException nếu tham số không hợp lệ
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

        existingVideo.setVideoUrl(videoUrl != null ? videoUrl.trim() : null);
        Video updatedVideo = videoRepository.save(existingVideo);

        log.info("Video URL updated successfully for video ID: {} by user: {}", 
                id, jwtTokenService.getCurrentUserNameFromJwt());
        return mapToResponseDto(updatedVideo);
    }

    /**
     * Xóa video theo ID
     */
    @Transactional
    public void deleteVideo(Long id) {
        log.info("Deleting video with ID: {}", id);

//        if (!videoRepository.existsById(id)) {
//            throw new VideoNotFoundException("Không tìm thấy video với ID: " + id);
//        }

        Video video = videoRepository.findById(id).orElseThrow(
                () -> new VideoNotFoundException("Không tìm thấy video với ID: " + id)
        );

        video.setIsDeleted(true);

        videoRepository.save(video);
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
                                               VideoStatus videoStatus, String assignedStaff, DeliveryStatus deliveryStatus, PaymentStatus paymentStatus) {
        log.info("Fetching all videos - page: {}, size: {}, sortBy: {}, direction: {}",
                page, size, sortBy, sortDirection);

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Video> videoPage = videoRepository.getAll(pageable, videoStatus, assignedStaff, deliveryStatus, paymentStatus);

        return videoPage.map(this::mapToResponseDto);
    }

    /**
     * Lấy danh sách video không phân trang
     */
    public List<VideoResponseDto> getAllVideos() {
        log.info("Fetching all videos without pagination");

        List<Video> videos = videoRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        return videos.stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    /**
     * Tìm kiếm video theo tên khách hàng (không phân biệt hoa thường)
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

        return videos.stream()
                .map(this::mapToResponseDto)
                .toList();
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

        return videos.stream()
                .map(this::mapToResponseDto)
                .toList();
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

        return videos.stream()
                .map(this::mapToResponseDto)
                .toList();
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

        return videos.stream()
                .map(this::mapToResponseDto)
                .toList();
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

        return videos.stream()
                .map(this::mapToResponseDto)
                .toList();
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

        existingVideo.setDeliveryStatus(status);
        Video updatedVideo = videoRepository.save(existingVideo);

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

        existingVideo.setPaymentStatus(status);

        // Tự động set paymentDate khi status = DA_THANH_TOAN
        if (status == PaymentStatus.DA_THANH_TOAN && existingVideo.getPaymentDate() == null) {
            existingVideo.setPaymentDate(LocalDateTime.now());
        }

        Video updatedVideo = videoRepository.save(existingVideo);

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
     * Tính tổng tiền lương cho các nhân viên
     * Chỉ tính các video đã thanh toán
     * Có thể lọc theo ngày thanh toán
     *
     * @param date Ngày cần thống kê (có thể null để lấy tất cả)
     * @return Danh sách thông tin lương của từng nhân viên
     */
    public List<StaffSalaryDto> calculateStaffSalaries(LocalDate date) {
        log.info("Calculating staff salaries for date: {}", date);
        List<StaffSalaryDto> salaries = videoRepository.calculateStaffSalaries(date);
        log.info("Found salary information for {} staff members", salaries.size());
        return salaries;
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