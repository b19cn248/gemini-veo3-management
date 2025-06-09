package com.ptit.google.veo3.service;

import com.ptit.google.veo3.dto.VideoRequestDto;
import com.ptit.google.veo3.dto.VideoResponseDto;
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
     * Xóa video theo ID
     */
    @Transactional
    public void deleteVideo(Long id) {
        log.info("Deleting video with ID: {}", id);

        if (!videoRepository.existsById(id)) {
            throw new VideoNotFoundException("Không tìm thấy video với ID: " + id);
        }

        videoRepository.deleteById(id);
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
    public Page<VideoResponseDto> getAllVideos(int page, int size, String sortBy, String sortDirection) {
        log.info("Fetching all videos - page: {}, size: {}, sortBy: {}, direction: {}",
                page, size, sortBy, sortDirection);

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Video> videoPage = videoRepository.findAll(pageable);

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
     *
     * @param customerName - Tên khách hàng cần tìm (có thể là một phần của tên)
     * @return List<VideoResponseDto> - Danh sách video tìm được
     */
    public List<VideoResponseDto> searchByCustomerName(String customerName) {
        log.info("Searching videos by customer name: '{}'", customerName);

        if (!StringUtils.hasText(customerName)) {
            log.warn("Customer name is empty or null, returning empty list");
            return List.of();
        }

        // Trim và validate input
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
     *
     * @param statusString - Trạng thái video dưới dạng string
     * @return List<VideoResponseDto> - Danh sách video có trạng thái tương ứng
     */
    public List<VideoResponseDto> getVideosByStatus(String statusString) {
        log.info("Fetching videos by status: '{}'", statusString);

        if (!StringUtils.hasText(statusString)) {
            throw new IllegalArgumentException("Trạng thái không được để trống");
        }

        VideoStatus status;
        try {
            // Chuyển đổi string thành enum, không phân biệt hoa thường
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
     *
     * @param assignedStaff - Tên nhân viên được giao (có thể là một phần của tên)
     * @return List<VideoResponseDto> - Danh sách video được giao cho nhân viên
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
     *
     * @return Map<VideoStatus, Long> - Map chứa số lượng video cho mỗi trạng thái
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
     *
     * @param startDate - Ngày bắt đầu
     * @param endDate - Ngày kết thúc
     * @param page - Số trang
     * @param size - Kích thước trang
     * @return Page<VideoResponseDto> - Danh sách video trong khoảng thời gian
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
     *
     * @param customerName - Tên khách hàng (optional)
     * @param status - Trạng thái video (optional)
     * @param assignedStaff - Nhân viên được giao (optional)
     * @param startDate - Ngày tạo từ (optional)
     * @param endDate - Ngày tạo đến (optional)
     * @return List<VideoResponseDto> - Danh sách video thỏa mãn điều kiện
     */
    public List<VideoResponseDto> advancedSearch(String customerName, VideoStatus status,
                                                 String assignedStaff, LocalDateTime startDate,
                                                 LocalDateTime endDate) {
        log.info("Performing advanced search with filters - customer: '{}', status: '{}', staff: '{}', date range: {} to {}",
                customerName, status, assignedStaff, startDate, endDate);

        // Bắt đầu với tất cả video
        List<Video> videos = videoRepository.findAll();

        // Áp dụng các filter theo thứ tự
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
                    // Tự động set paymentDate khi trạng thái là "Đã thanh toán"
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
