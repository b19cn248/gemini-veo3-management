package com.ptit.google.veo3.controller;

import com.ptit.google.veo3.dto.VideoCreateRequest;
import com.ptit.google.veo3.entity.Video;
import com.ptit.google.veo3.service.UserService;
import com.ptit.google.veo3.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class VideoController {

    private final VideoService videoService;
    private final UserService userService;

    /**
     * Tạo mới video record
     * POST /api/videos
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createVideo(@Valid @RequestBody VideoCreateRequest request) {
        log.info("Received request to create video for customer: {}", request.getCustomerName());

        try {
            Video createdVideo = videoService.createVideo(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Video đã được tạo thành công");
            response.put("data", createdVideo);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error creating video", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi tạo video: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Cập nhật video record
     * PUT /api/videos/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateVideo(
            @PathVariable Long id,
            @Valid @RequestBody VideoCreateRequest request) {

        log.info("Received request to update video with ID: {}", id);

        try {
            return videoService.updateVideo(id, request)
                    .map(updatedVideo -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("message", "Video đã được cập nhật thành công");
                        response.put("data", updatedVideo);

                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "Không tìm thấy video với ID: " + id);

                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                    });
        } catch (Exception e) {
            log.error("Error updating video", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi cập nhật video: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Xóa video record
     * DELETE /api/videos/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteVideo(@PathVariable Long id) {
        log.info("Received request to delete video with ID: {}", id);

        Map<String, Object> response = new HashMap<>();

        if (videoService.deleteVideo(id)) {
            response.put("success", true);
            response.put("message", "Video đã được xóa thành công");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Không tìm thấy video với ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Lấy chi tiết video theo ID
     * GET /api/videos/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getVideoById(@PathVariable Long id) {
        log.info("Received request to get video with ID: {}", id);

        return videoService.getVideoById(id)
                .map(video -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", video);

                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Không tìm thấy video với ID: " + id);

                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                });
    }

    /**
     * Lấy danh sách tất cả video với phân trang
     * GET /api/videos?page=0&size=10&sort=createdAt,desc
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Received request to get all videos with pagination: page={}, size={}", page, size);

        try {
            Sort sort = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Video> videoPage = videoService.getAllVideos(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", videoPage.getContent());
            response.put("currentPage", videoPage.getNumber());
            response.put("totalItems", videoPage.getTotalElements());
            response.put("totalPages", videoPage.getTotalPages());
            response.put("pageSize", videoPage.getSize());
            response.put("hasNext", videoPage.hasNext());
            response.put("hasPrevious", videoPage.hasPrevious());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching videos", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi tải danh sách video: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Tìm video theo tên khách hàng
     * GET /api/videos/search?customerName=Nguyen
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchVideosByCustomerName(
            @RequestParam String customerName) {

        log.info("Received request to search videos by customer name: {}", customerName);

        try {
            List<Video> videos = videoService.findByCustomerName(customerName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", videos);
            response.put("totalItems", videos.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error searching videos by customer name", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi tìm kiếm: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Lấy video theo user được giao
     * GET /api/videos/user/{userId}?page=0&size=10
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getVideosByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Received request to get videos by user ID: {}", userId);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Video> videoPage = videoService.getVideosByUser(userId, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", videoPage.getContent());
            response.put("currentPage", videoPage.getNumber());
            response.put("totalItems", videoPage.getTotalElements());
            response.put("totalPages", videoPage.getTotalPages());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching videos by user", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi tải video: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Lấy thống kê video theo trạng thái
     * GET /api/videos/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getVideoStats() {
        log.info("Received request to get video statistics");

        try {
            List<Object[]> stats = videoService.getVideoStatsByStatus();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching video statistics", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi tải thống kê: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Lấy thống kê video theo user
     * GET /api/videos/stats/user
     */
    @GetMapping("/stats/user")
    public ResponseEntity<Map<String, Object>> getVideoStatsByUser() {
        log.info("Received request to get video statistics by user");

        try {
            List<Object[]> stats = videoService.getVideoStatsByUser();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching video statistics by user", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi tải thống kê: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Lấy danh sách users để gán video
     * GET /api/videos/available-users
     */
    @GetMapping("/available-users")
    public ResponseEntity<Map<String, Object>> getAvailableUsers() {
        log.info("Received request to get available users for video assignment");

        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userService.getActiveUsers());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching available users", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi tải danh sách nhân viên: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

