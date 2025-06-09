package com.ptit.google.veo3.controller;

import com.ptit.google.veo3.dto.VideoRequestDto;
import com.ptit.google.veo3.dto.VideoResponseDto;
import com.ptit.google.veo3.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller để xử lý các HTTP requests liên quan đến Video
 * Tuân theo RESTful API best practices
 *
 * Base URL: /api/v1/videos
 *
 * Endpoints:
 * - POST   /api/v1/videos                    - Tạo mới video
 * - PUT    /api/v1/videos/{id}               - Cập nhật video
 * - DELETE /api/v1/videos/{id}               - Xóa video
 * - GET    /api/v1/videos/{id}               - Lấy chi tiết video
 * - GET    /api/v1/videos                    - Lấy danh sách video (có phân trang)
 * - GET    /api/v1/videos/all                - Lấy tất cả video (không phân trang)
 * - GET    /api/v1/videos/search             - Tìm kiếm video theo tên khách hàng
 * - GET    /api/v1/videos/status/{status}    - Lấy video theo trạng thái
 * - PATCH  /api/v1/videos/{id}/assigned-staff - Cập nhật nhân viên được giao
 * - PATCH  /api/v1/videos/{id}/status        - Cập nhật trạng thái video
 */
@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Cho phép CORS từ mọi origin (production nên hạn chế)
public class VideoController {

    private final VideoService videoService;

    /**
     * POST /api/v1/videos - Tạo mới video
     *
     * @param requestDto - Dữ liệu video cần tạo (đã được validate)
     * @return ResponseEntity chứa thông tin video vừa tạo hoặc thông báo lỗi
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createVideo(@Valid @RequestBody VideoRequestDto requestDto) {
        log.info("Received request to create video for customer: {}", requestDto.getCustomerName());

        try {
            VideoResponseDto createdVideo = videoService.createVideo(requestDto);

            Map<String, Object> response = createSuccessResponse(
                    "Video được tạo thành công",
                    createdVideo
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error creating video: ", e);
            return createErrorResponse("Lỗi khi tạo video: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * PUT /api/v1/videos/{id} - Cập nhật video
     *
     * @param id - ID của video cần cập nhật
     * @param requestDto - Dữ liệu mới để cập nhật (đã được validate)
     * @return ResponseEntity chứa thông tin video sau khi cập nhật hoặc thông báo lỗi
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateVideo(
            @PathVariable Long id,
            @Valid @RequestBody VideoRequestDto requestDto) {

        log.info("Received request to update video with ID: {}", id);

        try {
            VideoResponseDto updatedVideo = videoService.updateVideo(id, requestDto);

            Map<String, Object> response = createSuccessResponse(
                    "Video được cập nhật thành công",
                    updatedVideo
            );

            return ResponseEntity.ok(response);

        } catch (VideoService.VideoNotFoundException e) {
            log.warn("Video not found with ID: {}", id);
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);

        } catch (Exception e) {
            log.error("Error updating video with ID {}: ", id, e);
            return createErrorResponse("Lỗi khi cập nhật video: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * PATCH /api/v1/videos/{id}/assigned-staff - Cập nhật nhân viên được giao
     *
     * @param id - ID của video cần cập nhật
     * @param assignedStaff - Tên nhân viên được giao mới
     * @return ResponseEntity chứa thông tin video sau khi cập nhật hoặc thông báo lỗi
     */
    @PatchMapping("/{id}/assigned-staff")
    public ResponseEntity<Map<String, Object>> updateAssignedStaff(
            @PathVariable Long id,
            @RequestParam String assignedStaff) {

        log.info("Received request to update assigned staff for video ID: {} to staff: {}", id, assignedStaff);

        try {
            VideoResponseDto updatedVideo = videoService.updateAssignedStaff(id, assignedStaff);

            Map<String, Object> response = createSuccessResponse(
                    "Nhân viên được giao đã được cập nhật thành công",
                    updatedVideo
            );

            return ResponseEntity.ok(response);

        } catch (VideoService.VideoNotFoundException e) {
            log.warn("Video not found with ID: {}", id);
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid assigned staff for video ID {}: {}", id, e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            log.error("Error updating assigned staff for video ID {}: ", id, e);
            return createErrorResponse("Lỗi khi cập nhật nhân viên được giao: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PATCH /api/v1/videos/{id}/status - Cập nhật trạng thái video
     *
     * @param id - ID của video cần cập nhật
     * @param status - Trạng thái mới của video
     * @return ResponseEntity chứa thông tin video sau khi cập nhật hoặc thông báo lỗi
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateVideoStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        log.info("Received request to update status for video ID: {} to status: {}", id, status);

        try {
            VideoResponseDto updatedVideo = videoService.updateVideoStatus(id, status);

            Map<String, Object> response = createSuccessResponse(
                    "Trạng thái video đã được cập nhật thành công",
                    updatedVideo
            );

            return ResponseEntity.ok(response);

        } catch (VideoService.VideoNotFoundException e) {
            log.warn("Video not found with ID: {}", id);
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid status for video ID {}: {}", id, e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            log.error("Error updating status for video ID {}: ", id, e);
            return createErrorResponse("Lỗi khi cập nhật trạng thái video: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * DELETE /api/v1/videos/{id} - Xóa video
     *
     * @param id - ID của video cần xóa
     * @return ResponseEntity chứa thông báo xóa thành công hoặc thông báo lỗi
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteVideo(@PathVariable Long id) {
        log.info("Received request to delete video with ID: {}", id);

        try {
            videoService.deleteVideo(id);

            Map<String, Object> response = createSuccessResponse(
                    "Video được xóa thành công",
                    null
            );

            return ResponseEntity.ok(response);

        } catch (VideoService.VideoNotFoundException e) {
            log.warn("Video not found with ID: {}", id);
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);

        } catch (Exception e) {
            log.error("Error deleting video with ID {}: ", id, e);
            return createErrorResponse("Lỗi khi xóa video: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/v1/videos/{id} - Lấy thông tin chi tiết video
     *
     * @param id - ID của video cần lấy thông tin
     * @return ResponseEntity chứa thông tin video hoặc thông báo lỗi
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getVideoById(@PathVariable Long id) {
        log.info("Received request to get video with ID: {}", id);

        try {
            VideoResponseDto video = videoService.getVideoById(id);

            Map<String, Object> response = createSuccessResponse(
                    "Lấy thông tin video thành công",
                    video
            );

            return ResponseEntity.ok(response);

        } catch (VideoService.VideoNotFoundException e) {
            log.warn("Video not found with ID: {}", id);
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);

        } catch (Exception e) {
            log.error("Error getting video with ID {}: ", id, e);
            return createErrorResponse("Lỗi khi lấy thông tin video: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/v1/videos - Lấy danh sách tất cả video (có phân trang)
     *
     * Query Parameters:
     * - page: Số trang (mặc định: 0)
     * - size: Kích thước trang (mặc định: 10)
     * - sortBy: Trường để sắp xếp (mặc định: createdAt)
     * - sortDirection: Hướng sắp xếp - asc/desc (mặc định: desc)
     *
     * @param page - Số trang cần lấy
     * @param size - Số lượng record trên mỗi trang
     * @param sortBy - Trường để sắp xếp
     * @param sortDirection - Hướng sắp xếp (asc hoặc desc)
     * @return ResponseEntity chứa danh sách video với thông tin phân trang
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("Received request to get all videos - page: {}, size: {}, sortBy: {}, direction: {}",
                page, size, sortBy, sortDirection);

        try {
            Page<VideoResponseDto> videoPage = videoService.getAllVideos(page, size, sortBy, sortDirection);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Lấy danh sách video thành công");
            response.put("data", videoPage.getContent());
            response.put("pagination", Map.of(
                    "currentPage", videoPage.getNumber(),
                    "totalPages", videoPage.getTotalPages(),
                    "totalElements", videoPage.getTotalElements(),
                    "pageSize", videoPage.getSize(),
                    "hasNext", videoPage.hasNext(),
                    "hasPrevious", videoPage.hasPrevious(),
                    "isFirst", videoPage.isFirst(),
                    "isLast", videoPage.isLast()
            ));
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting all videos: ", e);
            return createErrorResponse("Lỗi khi lấy danh sách video: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/v1/videos/all - Lấy tất cả video không phân trang
     * Thích hợp để lấy toàn bộ dữ liệu cho export hoặc các use case khác
     *
     * @return ResponseEntity chứa toàn bộ danh sách video
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllVideosWithoutPagination() {
        log.info("Received request to get all videos without pagination");

        try {
            List<VideoResponseDto> videos = videoService.getAllVideos();

            Map<String, Object> response = createSuccessResponse(
                    "Lấy danh sách video thành công",
                    videos
            );
            response.put("total", videos.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting all videos without pagination: ", e);
            return createErrorResponse("Lỗi khi lấy danh sách video: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/v1/videos/search - Tìm kiếm video theo tên khách hàng
     *
     * @param customerName - Tên khách hàng cần tìm (tìm kiếm gần đúng, không phân biệt hoa thường)
     * @return ResponseEntity chứa danh sách video tìm được
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchVideosByCustomerName(
            @RequestParam String customerName
    ) {

        log.info("Received request to search videos by customer name: {}", customerName);

        try {
            List<VideoResponseDto> videos = videoService.searchByCustomerName(customerName);

            Map<String, Object> response = createSuccessResponse(
                    String.format("Tìm thấy %d video cho khách hàng '%s'", videos.size(), customerName),
                    videos
            );
            response.put("total", videos.size());
            response.put("searchTerm", customerName);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error searching videos by customer name '{}': ", customerName, e);
            return createErrorResponse("Lỗi khi tìm kiếm video: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/v1/videos/status/{status} - Lấy danh sách video theo trạng thái
     *
     * @param status - Trạng thái video cần lọc
     * @return ResponseEntity chứa danh sách video có trạng thái tương ứng
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> getVideosByStatus(@PathVariable String status) {
        log.info("Received request to get videos by status: {}", status);

        try {
            List<VideoResponseDto> videos = videoService.getVideosByStatus(status);

            Map<String, Object> response = createSuccessResponse(
                    String.format("Lấy danh sách video có trạng thái '%s' thành công", status),
                    videos
            );
            response.put("total", videos.size());
            response.put("filterStatus", status);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting videos by status '{}': ", status, e);
            return createErrorResponse("Lỗi khi lấy video theo trạng thái: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Helper method để tạo response thành công với format nhất quán
     *
     * @param message - Thông báo thành công
     * @param data - Dữ liệu trả về
     * @return Map chứa thông tin response
     */
    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Helper method để tạo response lỗi với format nhất quán
     *
     * @param message - Thông báo lỗi
     * @param status - HTTP status code
     * @return ResponseEntity chứa thông tin lỗi
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("data", null);
        response.put("timestamp", System.currentTimeMillis());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Exception handler để xử lý validation errors từ @Valid annotation
     * Tự động bắt các lỗi validation và trả về response có cấu trúc
     *
     * @param ex - Exception chứa thông tin validation errors
     * @return ResponseEntity chứa chi tiết các lỗi validation
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {

        log.warn("Validation error occurred: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Dữ liệu đầu vào không hợp lệ");
        response.put("errors", errors);
        response.put("data", null);
        response.put("timestamp", System.currentTimeMillis());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Bad Request");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Exception handler để xử lý các runtime exceptions khác
     *
     * @param ex - Runtime exception
     * @return ResponseEntity chứa thông tin lỗi generic
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception occurred: ", ex);
        return createErrorResponse("Đã xảy ra lỗi hệ thống: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Exception handler cho IllegalArgumentException
     *
     * @param ex - IllegalArgumentException
     * @return ResponseEntity chứa thông tin lỗi
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument exception: {}", ex.getMessage());
        return createErrorResponse("Tham số không hợp lệ: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
}