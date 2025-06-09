package com.ptit.google.veo3.service;

import com.ptit.google.veo3.dto.VideoCreateRequest;
import com.ptit.google.veo3.entity.PaymentStatus;
import com.ptit.google.veo3.entity.User;
import com.ptit.google.veo3.entity.Video;
import com.ptit.google.veo3.repository.UserRepository;
import com.ptit.google.veo3.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    /**
     * Tạo mới video record
     */
    public Video createVideo(VideoCreateRequest request) {
        log.info("Creating new video for customer: {}", request.getCustomerName());

        // Build video entity
        Video.VideoBuilder videoBuilder = Video.builder()
                .customerName(request.getCustomerName())
                .videoContent(request.getVideoContent())
                .imageUrl(request.getImageUrl())
                .videoDuration(request.getVideoDuration())
                .deliveryTime(request.getDeliveryTime())
                .status(request.getStatus())
                .videoUrl(request.getVideoUrl())
                .completedTime(request.getCompletedTime())
                .customerApproved(request.getCustomerApproved())
                .customerNote(request.getCustomerNote())
                .checked(request.getChecked())
                .deliveryStatus(request.getDeliveryStatus())
                .paymentStatus(request.getPaymentStatus())
                .paymentDate(request.getPaymentDate())
                .orderValue(request.getOrderValue());

        // Gán user nếu có
        if (request.getAssignedUserId() != null) {
            User assignedUser = userRepository.findById(request.getAssignedUserId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với ID: " + request.getAssignedUserId()));
            videoBuilder.assignedUser(assignedUser);
        }

        Video video = videoBuilder.build();

        // Validate payment date logic
        if (video.getPaymentStatus() == PaymentStatus.DA_THANH_TOAN && video.getPaymentDate() == null) {
            video.setPaymentDate(LocalDateTime.now());
        }

        Video savedVideo = videoRepository.save(video);
        log.info("Created video with ID: {}", savedVideo.getId());
        return savedVideo;
    }

    /**
     * Cập nhật video record
     */
    public Optional<Video> updateVideo(Long id, VideoCreateRequest request) {
        log.info("Updating video with ID: {}", id);

        return videoRepository.findById(id)
                .map(existingVideo -> {
                    // Update fields
                    existingVideo.setCustomerName(request.getCustomerName());
                    existingVideo.setVideoContent(request.getVideoContent());
                    existingVideo.setImageUrl(request.getImageUrl());
                    existingVideo.setVideoDuration(request.getVideoDuration());
                    existingVideo.setDeliveryTime(request.getDeliveryTime());
                    existingVideo.setStatus(request.getStatus());
                    existingVideo.setVideoUrl(request.getVideoUrl());
                    existingVideo.setCompletedTime(request.getCompletedTime());
                    existingVideo.setCustomerApproved(request.getCustomerApproved());
                    existingVideo.setCustomerNote(request.getCustomerNote());
                    existingVideo.setChecked(request.getChecked());
                    existingVideo.setDeliveryStatus(request.getDeliveryStatus());
                    existingVideo.setPaymentStatus(request.getPaymentStatus());
                    existingVideo.setPaymentDate(request.getPaymentDate());
                    existingVideo.setOrderValue(request.getOrderValue());

                    // Update assigned user
                    if (request.getAssignedUserId() != null) {
                        User assignedUser = userRepository.findById(request.getAssignedUserId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với ID: " + request.getAssignedUserId()));
                        existingVideo.setAssignedUser(assignedUser);
                    } else {
                        existingVideo.setAssignedUser(null);
                    }

                    // Handle payment date logic
                    if (request.getPaymentStatus() == PaymentStatus.DA_THANH_TOAN
                            && existingVideo.getPaymentDate() == null) {
                        existingVideo.setPaymentDate(LocalDateTime.now());
                    } else if (request.getPaymentStatus() != PaymentStatus.DA_THANH_TOAN) {
                        existingVideo.setPaymentDate(null);
                    }

                    Video updatedVideo = videoRepository.save(existingVideo);
                    log.info("Updated video with ID: {}", updatedVideo.getId());
                    return updatedVideo;
                });
    }

    /**
     * Xóa video record
     */
    public boolean deleteVideo(Long id) {
        log.info("Deleting video with ID: {}", id);

        if (videoRepository.existsById(id)) {
            videoRepository.deleteById(id);
            log.info("Deleted video with ID: {}", id);
            return true;
        }

        log.warn("Video with ID: {} not found for deletion", id);
        return false;
    }

    /**
     * Lấy chi tiết video theo ID
     */
    @Transactional(readOnly = true)
    public Optional<Video> getVideoById(Long id) {
        log.info("Fetching video with ID: {}", id);
        return videoRepository.findById(id);
    }

    /**
     * Lấy danh sách tất cả video với phân trang
     */
    @Transactional(readOnly = true)
    public Page<Video> getAllVideos(Pageable pageable) {
        log.info("Fetching all videos with pagination");
        return videoRepository.findAll(pageable);
    }

    /**
     * Tìm video theo tên khách hàng
     */
    @Transactional(readOnly = true)
    public List<Video> findByCustomerName(String customerName) {
        log.info("Searching videos by customer name: {}", customerName);
        return videoRepository.findByCustomerNameContainingIgnoreCase(customerName);
    }

    /**
     * Lấy video theo user được giao
     */
    @Transactional(readOnly = true)
    public Page<Video> getVideosByUser(Long userId, Pageable pageable) {
        log.info("Fetching videos by user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với ID: " + userId));
        return videoRepository.findByAssignedUser(user, pageable);
    }

    /**
     * Lấy thống kê số lượng video theo trạng thái
     */
    @Transactional(readOnly = true)
    public List<Object[]> getVideoStatsByStatus() {
        log.info("Fetching video statistics by status");
        return videoRepository.countVideosByStatus();
    }

    /**
     * Lấy thống kê số lượng video theo user
     */
    @Transactional(readOnly = true)
    public List<Object[]> getVideoStatsByUser() {
        log.info("Fetching video statistics by user");
        return videoRepository.countVideosByUser();
    }
}
