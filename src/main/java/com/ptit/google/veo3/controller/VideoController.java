package com.ptit.google.veo3.controller;

import com.ptit.google.veo3.dto.StaffSalaryDto;
import com.ptit.google.veo3.dto.VideoRequestDto;
import com.ptit.google.veo3.dto.VideoResponseDto;
import com.ptit.google.veo3.entity.DeliveryStatus;
import com.ptit.google.veo3.entity.PaymentStatus;
import com.ptit.google.veo3.entity.Video;
import com.ptit.google.veo3.entity.VideoStatus;
import com.ptit.google.veo3.multitenant.TenantContext;
import com.ptit.google.veo3.service.JwtTokenService;
import com.ptit.google.veo3.service.StaffWorkloadService;
import com.ptit.google.veo3.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller để xử lý các HTTP requests liên quan đến Video
 * Tuân theo RESTful API best practices
 * <p>
 * Updated để support multi-tenant với tenant context logging
 * <p>
 * Base URL: /api/v1/videos
 * <p>
 * Endpoints:
 * - POST   /api/v1/videos                    - Tạo mới video
 * - PUT    /api/v1/videos/{id}               - Cập nhật video
 * - DELETE /api/v1/videos/{id}               - Xóa video
 * - GET    /api/v1/videos/{id}               - Lấy chi tiết video
 * - GET    /api/v1/videos                    - Lấy danh sách video (có phân trang)
 * - GET    /api/v1/videos/all                - Lấy tất cả video (không phân trang)
 * - GET    /api/v1/videos/search             - Tìm kiếm video theo tên khách hàng
 * - GET    /api/v1/videos/status/{status}    - Lấy video theo trạng thái
 * - PATCH  /api/v1/videos/{id}/assigned-staff - Cập nhật nhân viên được giao (MAX: 3 videos)
 * - PATCH  /api/v1/videos/{id}/status        - Cập nhật trạng thái video (PERMISSION: chỉ assignedStaff)
 * - PATCH  /api/v1/videos/{id}/video-url     - Cập nhật link video (PERMISSION: chỉ assignedStaff)
 * - PUT    /api/v1/videos/{id}/delivery-status - Cập nhật trạng thái giao hàng
 * - PUT    /api/v1/videos/{id}/payment-status  - Cập nhật trạng thái thanh toán
 * - GET    /api/v1/videos/staff-workload     - Lấy thông tin workload của nhân viên
 */
@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Cho phép CORS từ mọi origin (production nên hạn chế)
public class VideoController {

    private final VideoService videoService;
    private final JwtTokenService jwtTokenService;
    private final StaffWorkloadService staffWorkloadService;

    /**
     * POST /api/v1/videos - Tạo mới video
     *
     * @param requestDto - Dữ liệu video cần tạo (đã được validate)
     * @return ResponseEntity chứa thông tin video vừa tạo hoặc thông báo lỗi
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createVideo(
            @Valid @RequestBody VideoRequestDto requestDto,
            @RequestHeader(value = "db", required = false) String dbHeader
    ) {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to create video for customer: {}", tenantId, requestDto.getCustomerName());

        try {

            Integer time = requestDto.getVideoDuration();

            Integer price = 0;

            if (time == 8) {
                price = 15000;
            }
            if (time == 16) {
                price = 45000;
            }
            if (time == 24) {
                price = 65000;
            }
            if (time == 32) {
                price = 100000;
            }

            requestDto.setOrderValue(BigDecimal.valueOf(price));

            VideoResponseDto createdVideo = videoService.createVideo(requestDto);

            Map<String, Object> response = createSuccessResponse(
                    "Video được tạo thành công",
                    createdVideo
            );

            log.info("[Tenant: {}] Video created successfully with ID: {}", tenantId, createdVideo.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error creating video: ", tenantId, e);
            return createErrorResponse("Lỗi khi tạo video: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * PUT /api/v1/videos/{id} - Cập nhật video
     *
     * @param id         - ID của video cần cập nhật
     * @param requestDto - Dữ liệu mới để cập nhật (đã được validate)
     * @return ResponseEntity chứa thông tin video sau khi cập nhật hoặc thông báo lỗi
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateVideo(
            @PathVariable Long id,
            @RequestHeader(value = "db", required = false) String dbHeader,
            @Valid @RequestBody VideoRequestDto requestDto) {

        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to update video with ID: {}", tenantId, id);

        Integer time = requestDto.getVideoDuration();

        Integer price = 0;

        if (time == 8) {
            price = 15000;
        }
        if (time == 16) {
            price = 45000;
        }
        if (time == 24) {
            price = 65000;
        }
        if (time == 32) {
            price = 100000;
        }

        requestDto.setOrderValue(BigDecimal.valueOf(price));

        try {
            VideoResponseDto updatedVideo = videoService.updateVideo(id, requestDto);

            Map<String, Object> response = createSuccessResponse(
                    "Video được cập nhật thành công",
                    updatedVideo
            );

            log.info("[Tenant: {}] Video updated successfully with ID: {}", tenantId, id);
            return ResponseEntity.ok(response);

        } catch (VideoService.VideoNotFoundException e) {
            log.warn("[Tenant: {}] Video not found with ID: {}", tenantId, id);
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error updating video with ID {}: ", tenantId, id, e);
            return createErrorResponse("Lỗi khi cập nhật video: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * PATCH /api/v1/videos/{id}/assigned-staff - Cập nhật nhân viên được giao
     *
     * @param id            - ID của video cần cập nhật
     * @param assignedStaff - Tên nhân viên được giao mới
     * @return ResponseEntity chứa thông tin video sau khi cập nhật hoặc thông báo lỗi
     */
    @PatchMapping("/{id}/assigned-staff")
    public ResponseEntity<Map<String, Object>> updateAssignedStaff(
            @PathVariable Long id,
            @RequestParam String assignedStaff) {

        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to update assigned staff for video ID: {} to staff: {}",
                tenantId, id, assignedStaff);

        try {
            VideoResponseDto updatedVideo = videoService.updateAssignedStaff(id, assignedStaff);

            Map<String, Object> response = createSuccessResponse(
                    "Nhân viên được giao đã được cập nhật thành công",
                    updatedVideo
            );

            return ResponseEntity.ok(response);

        } catch (VideoService.VideoNotFoundException e) {
            log.warn("[Tenant: {}] Video not found with ID: {}", tenantId, id);
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);

        } catch (IllegalArgumentException e) {
            log.warn("[Tenant: {}] Invalid assigned staff for video ID {}: {}", tenantId, id, e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error updating assigned staff for video ID {}: ", tenantId, id, e);
            return createErrorResponse("Lỗi khi cập nhật nhân viên được giao: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PATCH Cập nhật trạng thái video - Cập nhật trạng thái video
     * 
     * SECURITY: Chỉ có nhân viên được giao video mới có quyền cập nhật trạng thái
     * Logic phân quyền: So sánh trường "name" từ JWT token với assignedStaff của video
     *
     * @param id     - ID của video cần cập nhật
     * @param status - Trạng thái mới của video
     * @return ResponseEntity chứa thông tin video sau khi cập nhật hoặc thông báo lỗi
     *         - 200 OK: Cập nhật thành công
     *         - 403 FORBIDDEN: Không có quyền cập nhật (không phải người được giao)
     *         - 404 NOT_FOUND: Không tìm thấy video
     *         - 400 BAD_REQUEST: Tham số không hợp lệ
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateVideoStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to update status for video ID: {} to status: {}",
                tenantId, id, status);

        try {
            VideoResponseDto updatedVideo = videoService.updateVideoStatus(id, status);

            Map<String, Object> response = createSuccessResponse(
                    "Trạng thái video đã được cập nhật thành công",
                    updatedVideo
            );

            return ResponseEntity.ok(response);

        } catch (VideoService.VideoNotFoundException e) {
            log.warn("[Tenant: {}] Video not found with ID: {}", tenantId, id);
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);

        } catch (SecurityException e) {
            log.warn("[Tenant: {}] Access denied for video status update - video ID: {}, error: {}", tenantId, id, e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);

        } catch (IllegalArgumentException e) {
            log.warn("[Tenant: {}] Invalid status for video ID {}: {}", tenantId, id, e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error updating status for video ID {}: ", tenantId, id, e);
            return createErrorResponse("Lỗi khi cập nhật trạng thái video: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PATCH /api/v1/videos/{id}/video-url - Cập nhật link video
     * 
     * SECURITY: Chỉ có nhân viên được giao video mới có quyền cập nhật video URL
     * Logic phân quyền: So sánh trường "name" từ JWT token với assignedStaff của video
     *
     * @param id       - ID của video cần cập nhật
     * @param videoUrl - Link video mới
     * @return ResponseEntity chứa thông tin video sau khi cập nhật hoặc thông báo lỗi
     *         - 200 OK: Cập nhật thành công
     *         - 403 FORBIDDEN: Không có quyền cập nhật (không phải người được giao)
     *         - 404 NOT_FOUND: Không tìm thấy video
     *         - 400 BAD_REQUEST: Tham số không hợp lệ
     */
    @PatchMapping("/{id}/video-url")
    public ResponseEntity<Map<String, Object>> updateVideoUrl(
            @PathVariable Long id,
            @RequestParam String videoUrl) {

        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to update video URL for video ID: {} to URL: {}",
                tenantId, id, videoUrl);

        try {
            VideoResponseDto updatedVideo = videoService.updateVideoUrl(id, videoUrl);

            Map<String, Object> response = createSuccessResponse(
                    "Link video đã được cập nhật thành công",
                    updatedVideo
            );

            return ResponseEntity.ok(response);

        } catch (VideoService.VideoNotFoundException e) {
            log.warn("[Tenant: {}] Video not found with ID: {}", tenantId, id);
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);

        } catch (SecurityException e) {
            log.warn("[Tenant: {}] Access denied for video URL update - video ID: {}, error: {}", tenantId, id, e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);

        } catch (IllegalArgumentException e) {
            log.warn("[Tenant: {}] Invalid video URL for video ID {}: {}", tenantId, id, e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error updating video URL for video ID {}: ", tenantId, id, e);
            return createErrorResponse("Lỗi khi cập nhật link video: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to delete video with ID: {}", tenantId, id);

        try {
            videoService.deleteVideo(id);

            Map<String, Object> response = createSuccessResponse(
                    "Video được xóa thành công",
                    null
            );

            return ResponseEntity.ok(response);

        } catch (VideoService.VideoNotFoundException e) {
            log.warn("[Tenant: {}] Video not found with ID: {}", tenantId, id);
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error deleting video with ID {}: ", tenantId, id, e);
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
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to get video with ID: {}", tenantId, id);

        try {
            VideoResponseDto video = videoService.getVideoById(id);

            Map<String, Object> response = createSuccessResponse(
                    "Lấy thông tin video thành công",
                    video
            );

            return ResponseEntity.ok(response);

        } catch (VideoService.VideoNotFoundException e) {
            log.warn("[Tenant: {}] Video not found with ID: {}", tenantId, id);
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error getting video with ID {}: ", tenantId, id, e);
            return createErrorResponse("Lỗi khi lấy thông tin video: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/v1/videos - Lấy danh sách tất cả video (có phân trang)
     * <p>
     * Query Parameters:
     * - page: Số trang (mặc định: 0)
     * - size: Kích thước trang (mặc định: 10)
     * - sortBy: Trường để sắp xếp (mặc định: createdAt)
     * - sortDirection: Hướng sắp xếp - asc/desc (mặc định: desc)
     *
     * @param page          - Số trang cần lấy
     * @param size          - Số lượng record trên mỗi trang
     * @param sortBy        - Trường để sắp xếp
     * @param sortDirection - Hướng sắp xếp (asc hoặc desc)
     * @return ResponseEntity chứa danh sách video với thông tin phân trang
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) VideoStatus status,
            @RequestParam(required = false) String assignedStaff,
            @RequestParam(required = false) DeliveryStatus deliveryStatus,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestHeader(value = "db", required = false) String dbHeader) {

        String tenantId = TenantContext.getTenantId();

        // In ra value của header "db"
        log.info("Header 'db' value: {}", dbHeader);

        log.info("[Tenant: {}] Received request to get all videos - page: {}, size: {}, sortBy: {}, direction: {}",
                tenantId, page, size, sortBy, sortDirection);

        try {
            Page<VideoResponseDto> videoPage = videoService.getAllVideos(page, size, sortBy, sortDirection,
                    status, assignedStaff, deliveryStatus, paymentStatus);

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
            response.put("tenantId", tenantId);
            response.put("dbHeader", dbHeader); // Thêm header value vào response
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error getting all videos: ", tenantId, e);
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
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to get all videos without pagination", tenantId);

        try {
            List<VideoResponseDto> videos = videoService.getAllVideos();

            Map<String, Object> response = createSuccessResponse(
                    "Lấy danh sách video thành công",
                    videos
            );
            response.put("total", videos.size());
            response.put("tenantId", tenantId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error getting all videos without pagination: ", tenantId, e);
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
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to search videos by customer name: {}", tenantId, customerName);

        try {
            List<VideoResponseDto> videos = videoService.searchByCustomerName(customerName);

            Map<String, Object> response = createSuccessResponse(
                    String.format("Tìm thấy %d video cho khách hàng '%s'", videos.size(), customerName),
                    videos
            );
            response.put("total", videos.size());
            response.put("searchTerm", customerName);
            response.put("tenantId", tenantId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error searching videos by customer name '{}': ", tenantId, customerName, e);
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
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to get videos by status: {}", tenantId, status);

        try {
            List<VideoResponseDto> videos = videoService.getVideosByStatus(status);

            Map<String, Object> response = createSuccessResponse(
                    String.format("Lấy danh sách video có trạng thái '%s' thành công", status),
                    videos
            );
            response.put("total", videos.size());
            response.put("filterStatus", status);
            response.put("tenantId", tenantId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error getting videos by status '{}': ", tenantId, status, e);
            return createErrorResponse("Lỗi khi lấy video theo trạng thái: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * GET /api/v1/videos/filter - Lọc video theo nhân viên, trạng thái giao hàng và trạng thái thanh toán
     * <p>
     * Query Parameters:
     * - assignedStaff: Tên nhân viên được giao (optional)
     * - deliveryStatus: Trạng thái giao hàng (optional)
     * - paymentStatus: Trạng thái thanh toán (optional)
     *
     * @param assignedStaff  - Tên nhân viên được giao
     * @param deliveryStatus - Trạng thái giao hàng
     * @param paymentStatus  - Trạng thái thanh toán
     * @return ResponseEntity chứa danh sách video đã được lọc
     */
    @GetMapping("/filter")
    public ResponseEntity<Map<String, Object>> filterVideos(
            @RequestParam(required = false) String assignedStaff,
            @RequestParam(required = false) String deliveryStatus,
            @RequestParam(required = false) String paymentStatus) {

        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to filter videos - staff: {}, delivery status: {}, payment status: {}",
                tenantId, assignedStaff, deliveryStatus, paymentStatus);

        try {
            List<VideoResponseDto> videos = videoService.filterVideos(assignedStaff, deliveryStatus, paymentStatus);

            Map<String, Object> response = createSuccessResponse(
                    "Lọc video thành công",
                    videos
            );
            response.put("total", videos.size());
            response.put("filters", Map.of(
                    "assignedStaff", assignedStaff,
                    "deliveryStatus", deliveryStatus,
                    "paymentStatus", paymentStatus
            ));
            response.put("tenantId", tenantId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("[Tenant: {}] Invalid filter parameters: {}", tenantId, e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error filtering videos: ", tenantId, e);
            return createErrorResponse("Lỗi khi lọc video: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PUT /api/v1/videos/{id}/delivery-status - Cập nhật trạng thái giao hàng
     *
     * @param id     - ID của video cần cập nhật
     * @param status - Trạng thái giao hàng mới
     * @return ResponseEntity chứa thông tin video sau khi cập nhật hoặc thông báo lỗi
     */
    @PutMapping("/{id}/delivery-status")
    public ResponseEntity<Map<String, Object>> updateDeliveryStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to update delivery status for video ID: {} to status: {}",
                tenantId, id, status);

        try {
            VideoResponseDto updatedVideo = videoService.updateDeliveryStatus(id, status);

            Map<String, Object> response = createSuccessResponse(
                    "Trạng thái giao hàng đã được cập nhật thành công",
                    updatedVideo
            );

            return ResponseEntity.ok(response);

        } catch (VideoService.VideoNotFoundException e) {
            log.warn("[Tenant: {}] Video not found with ID: {}", tenantId, id);
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);

        } catch (IllegalArgumentException e) {
            log.warn("[Tenant: {}] Invalid delivery status for video ID {}: {}", tenantId, id, e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error updating delivery status for video ID {}: ", tenantId, id, e);
            return createErrorResponse("Lỗi khi cập nhật trạng thái giao hàng: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PUT /api/v1/videos/{id}/payment-status - Cập nhật trạng thái thanh toán
     *
     * @param id     - ID của video cần cập nhật
     * @param status - Trạng thái thanh toán mới
     * @return ResponseEntity chứa thông tin video sau khi cập nhật hoặc thông báo lỗi
     */
    @PutMapping("/{id}/payment-status")
    public ResponseEntity<Map<String, Object>> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to update payment status for video ID: {} to status: {}",
                tenantId, id, status);

        try {
            VideoResponseDto updatedVideo = videoService.updatePaymentStatus(id, status);

            Map<String, Object> response = createSuccessResponse(
                    "Trạng thái thanh toán đã được cập nhật thành công",
                    updatedVideo
            );

            return ResponseEntity.ok(response);

        } catch (VideoService.VideoNotFoundException e) {
            log.warn("[Tenant: {}] Video not found with ID: {}", tenantId, id);
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);

        } catch (IllegalArgumentException e) {
            log.warn("[Tenant: {}] Invalid payment status for video ID {}: {}", tenantId, id, e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error updating payment status for video ID {}: ", tenantId, id, e);
            return createErrorResponse("Lỗi khi cập nhật trạng thái thanh toán: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/v1/videos/assigned-staff - Lấy danh sách các nhân viên được giao khác nhau
     *
     * @return ResponseEntity chứa danh sách tên nhân viên
     */
    @GetMapping("/assigned-staff")
    public ResponseEntity<Map<String, Object>> getDistinctAssignedStaff() {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to get distinct assigned staff names", tenantId);

        try {
            List<String> staffList = videoService.getDistinctAssignedStaff();

            List<String> staffNames = new ArrayList<>();

            for (String staff : staffList) {
                if (staff.isBlank()) {
                    staffNames.add("Chưa ai nhận");
                } else {
                    staffNames.add(staff);
                }
            }

            Map<String, Object> response = createSuccessResponse(
                    "Lấy danh sách nhân viên thành công",
                    staffNames
            );
            response.put("total", staffNames.size());
            response.put("tenantId", tenantId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error getting distinct assigned staff: ", tenantId, e);
            return createErrorResponse("Lỗi khi lấy danh sách nhân viên: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/v1/videos/staff-salaries - Lấy tổng tiền lương của các nhân viên
     * Chỉ tính các video đã thanh toán
     * Có thể lọc theo ngày thanh toán
     *
     * @param date Ngày cần thống kê (format: yyyy-MM-dd, optional)
     * @return ResponseEntity chứa danh sách tổng tiền lương của từng nhân viên
     */
    @GetMapping("/staff-salaries")
    public ResponseEntity<Map<String, Object>> getStaffSalaries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to get staff salaries for date: {}", tenantId, date);

        try {
            List<StaffSalaryDto> salaries = videoService.calculateStaffSalaries(date);

            // Tính tổng tiền lương của tất cả nhân viên
            BigDecimal totalSalary = salaries.stream()
                    .map(StaffSalaryDto::getTotalSalary)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Tính tổng số video đã thanh toán
            Long totalVideos = salaries.stream()
                    .mapToLong(StaffSalaryDto::getTotalVideos)
                    .sum();

            Map<String, Object> response = createSuccessResponse(
                    date != null ? 
                        String.format("Lấy thông tin lương nhân viên ngày %s thành công", date) :
                        "Lấy thông tin lương nhân viên thành công",
                    salaries
            );
            response.put("totalStaff", salaries.size());
            response.put("totalSalary", totalSalary);
            response.put("totalVideos", totalVideos);
            response.put("date", date);
            response.put("tenantId", tenantId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error getting staff salaries: ", tenantId, e);
            return createErrorResponse("Lỗi khi lấy thông tin lương nhân viên: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/v1/videos/staff-workload - Lấy thông tin workload của nhân viên
     * 
     * API mới để monitoring và debugging workload của từng nhân viên
     * Hữu ích cho việc theo dõi và quản lý phân bổ công việc
     *
     * @param staffName Tên nhân viên cần kiểm tra (optional - nếu không có sẽ lấy từ JWT)
     * @return ResponseEntity chứa thông tin workload chi tiết
     */
    @GetMapping("/staff-workload")
    public ResponseEntity<Map<String, Object>> getStaffWorkload(
            @RequestParam(required = false) String staffName) {
        String tenantId = TenantContext.getTenantId();
        
        try {
            // Nếu không có staffName thì lấy từ JWT của user hiện tại
            String targetStaff = staffName;
            if (!StringUtils.hasText(targetStaff)) {
                targetStaff = jwtTokenService.getCurrentUserNameFromJwt();
            }
            
            log.info("[Tenant: {}] Received request to get workload for staff: {}", tenantId, targetStaff);
            
            StaffWorkloadService.WorkloadInfo workloadInfo = staffWorkloadService.getWorkloadInfo(targetStaff);
            
            Map<String, Object> response = createSuccessResponse(
                    String.format("Lấy thông tin workload của nhân viên '%s' thành công", targetStaff),
                    Map.of(
                            "assignedStaff", workloadInfo.getAssignedStaff(),
                            "totalActive", workloadInfo.getTotalActive(),
                            "breakdown", Map.of(
                                    "dangLam", workloadInfo.getDangLamCount(),
                                    "dangSua", workloadInfo.getDangSuaCount(),
                                    "canSuaGap", workloadInfo.getCanSuaGapCount()
                            ),
                            "canAcceptNewTask", workloadInfo.isCanAcceptNewTask(),
                            "maxConcurrentVideos", workloadInfo.getMaxConcurrentVideos(),
                            "activeVideoIds", workloadInfo.getActiveVideos().stream()
                                    .map(Video::getId)
                                    .toList()
                    )
            );
            response.put("tenantId", tenantId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("[Tenant: {}] Error getting staff workload: ", tenantId, e);
            return createErrorResponse("Lỗi khi lấy thông tin workload: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Helper method để tạo response thành công với format nhất quán
     *
     * @param message - Thông báo thành công
     * @param data    - Dữ liệu trả về
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
     * @param status  - HTTP status code
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
        response.put("tenantId", TenantContext.getTenantId()); // Thêm tenant info vào error response
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

        String tenantId = TenantContext.getTenantId();
        log.warn("[Tenant: {}] Validation error occurred: {}", tenantId, ex.getMessage());

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
        response.put("tenantId", tenantId);

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
        String tenantId = TenantContext.getTenantId();
        log.error("[Tenant: {}] Runtime exception occurred: ", tenantId, ex);
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
        String tenantId = TenantContext.getTenantId();
        log.warn("[Tenant: {}] Illegal argument exception: {}", tenantId, ex.getMessage());
        return createErrorResponse("Tham số không hợp lệ: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Exception handler cho SecurityException
     * Xử lý các lỗi liên quan đến phân quyền truy cập
     *
     * @param ex - SecurityException
     * @return ResponseEntity chứa thông tin lỗi với HTTP status 403 FORBIDDEN
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException ex) {
        String tenantId = TenantContext.getTenantId();
        log.warn("[Tenant: {}] Security exception - access denied: {}", tenantId, ex.getMessage());
        return createErrorResponse("Không có quyền truy cập: " + ex.getMessage(), HttpStatus.FORBIDDEN);
    }
}