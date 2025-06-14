package com.ptit.google.veo3.controller;

import com.ptit.google.veo3.dto.StaffSalaryDto;
import com.ptit.google.veo3.dto.VideoRequestDto;
import com.ptit.google.veo3.dto.VideoResponseDto;
import com.ptit.google.veo3.dto.SalesSalaryDto;
import com.ptit.google.veo3.entity.DeliveryStatus;
import com.ptit.google.veo3.entity.PaymentStatus;
import com.ptit.google.veo3.entity.Video;
import com.ptit.google.veo3.entity.VideoStatus;
import com.ptit.google.veo3.multitenant.TenantContext;
import com.ptit.google.veo3.service.JwtTokenService;
import com.ptit.google.veo3.service.StaffWorkloadService;
import com.ptit.google.veo3.service.VideoService;
import com.ptit.google.veo3.service.VideoAutoResetService;
import com.ptit.google.veo3.util.VideoPricingUtil;
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
 * - GET    /api/v1/videos/sales-salaries    - Tính lương sales theo ngày thanh toán (NEW API)
 * - GET    /api/v1/videos/check-customer    - Kiểm tra khách hàng đã tồn tại (NEW API)
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
    private final VideoAutoResetService videoAutoResetService;

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

            Integer orderValueInt = 0;

            if (time == 8) {
                orderValueInt = 20000;
            }
            if (time == 16) {
                orderValueInt = 45000;
            }
            if (time == 24) {
                orderValueInt = 65000;
            }
            if (time == 32) {
                orderValueInt = 90000;
            }
            if (time == 40) {
                orderValueInt = 110000;
            }

            BigDecimal orderValue = BigDecimal.valueOf(orderValueInt);
            requestDto.setOrderValue(orderValue);
            
            // AUTO-CALCULATE PRICE: Sử dụng VideoPricingUtil để tính price
            BigDecimal calculatedPrice = VideoPricingUtil.calculatePrice(orderValue);
            if (calculatedPrice != null) {
                requestDto.setPrice(calculatedPrice);
                log.info("[Tenant: {}] Auto-calculated price {} for order value {} (customer: {})", 
                        tenantId, calculatedPrice, orderValue, requestDto.getCustomerName());
            } else {
                log.warn("[Tenant: {}] No pricing rule found for order value {} (customer: {})", 
                        tenantId, orderValue, requestDto.getCustomerName());
            }

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

        Integer orderValueInt = 0;

        if (time == 8) {
            orderValueInt = 20000;
        }
        if (time == 16) {
            orderValueInt = 45000;
        }
        if (time == 24) {
            orderValueInt = 65000;
        }
        if (time == 32) {
            orderValueInt = 90000;
        }
        if (time == 40) {
            orderValueInt = 110000;
        }

        BigDecimal orderValue = BigDecimal.valueOf(orderValueInt);
        requestDto.setOrderValue(orderValue);
        
        // AUTO-CALCULATE PRICE: Sử dụng VideoPricingUtil để tính price
        BigDecimal calculatedPrice = VideoPricingUtil.calculatePrice(orderValue);
        if (calculatedPrice != null) {
            requestDto.setPrice(calculatedPrice);
            log.info("[Tenant: {}] Auto-calculated price {} for order value {} during update (video ID: {})", 
                    tenantId, calculatedPrice, orderValue, id);
        } else {
            log.warn("[Tenant: {}] No pricing rule found for order value {} during update (video ID: {})", 
                    tenantId, orderValue, id);
        }

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
     * POST /api/v1/videos/{id}/cancel - Hủy video (ADMIN ONLY)
     * 
     * Reset video về trạng thái CHUA_AI_NHAN và assignedStaff = null
     * 
     * SECURITY: Chỉ có admin mới có quyền sử dụng API này
     * BUSINESS LOGIC: Reset video về trạng thái ban đầu
     *
     * @param id ID của video cần hủy
     * @return ResponseEntity chứa thông tin video sau khi hủy hoặc thông báo lỗi
     *         - 200 OK: Hủy thành công
     *         - 403 FORBIDDEN: Không phải admin
     *         - 404 NOT_FOUND: Không tìm thấy video
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelVideo(@PathVariable Long id) {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to cancel video ID: {}", tenantId, id);

        try {
            VideoResponseDto canceledVideo = videoService.cancelVideo(id);

            Map<String, Object> response = createSuccessResponse(
                    "Video đã được hủy thành công",
                    canceledVideo
            );

            return ResponseEntity.ok(response);

        } catch (VideoService.VideoNotFoundException e) {
            log.warn("[Tenant: {}] Video not found with ID: {}", tenantId, id);
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);

        } catch (SecurityException e) {
            log.warn("[Tenant: {}] Access denied for video cancel - video ID: {}, error: {}", 
                    tenantId, id, e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error canceling video ID {}: ", tenantId, id, e);
            return createErrorResponse("Lỗi khi hủy video: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
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
     * GET /api/v1/videos/sales-salaries - Tính lương sales theo ngày thanh toán
     * 
     * Business Logic:
     * - Lọc videos có paymentStatus = 'DA_THANH_TOAN'
     * - Lọc theo paymentDate = currentDate
     * - Group theo createdBy (sales person)  
     * - Tính tổng price per sales
     * - Hoa hồng = tổng price * 12%
     * 
     * @param currentDate Ngày hiện tại cần thống kê (format: yyyy-MM-dd, required)
     * @return ResponseEntity chứa danh sách lương sales theo ngày
     */
    @GetMapping("/sales-salaries")
    public ResponseEntity<Map<String, Object>> getSalesSalariesByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate currentDate) {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to calculate sales salaries for date: {}", tenantId, currentDate);

        try {
            List<SalesSalaryDto> salesSalaries = videoService.calculateSalesSalariesByDate(currentDate);

            // Tính tổng thống kê cho response
            BigDecimal totalCommission = salesSalaries.stream()
                    .map(SalesSalaryDto::getCommissionSalary)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalSalesValue = salesSalaries.stream()
                    .map(SalesSalaryDto::getTotalSalesValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Long totalVideos = salesSalaries.stream()
                    .mapToLong(SalesSalaryDto::getTotalPaidVideos)
                    .sum();

            Map<String, Object> response = createSuccessResponse(
                    String.format("Tính lương sales ngày %s thành công", currentDate),
                    salesSalaries
            );
            
            // Thêm thông tin thống kê tổng quan
            response.put("summary", Map.of(
                    "totalSalesPersons", salesSalaries.size(),
                    "totalCommission", totalCommission,
                    "totalSalesValue", totalSalesValue,
                    "totalPaidVideos", totalVideos,
                    "commissionRate", "12%",
                    "calculationDate", currentDate
            ));
            response.put("tenantId", tenantId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("[Tenant: {}] Invalid parameter for sales salary calculation: {}", tenantId, e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error calculating sales salaries for date {}: ", tenantId, currentDate, e);
            return createErrorResponse("Lỗi khi tính lương sales: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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
     * GET /api/v1/videos/auto-reset/status - Lấy thông tin trạng thái auto-reset system
     * 
     * API monitoring để kiểm tra:
     * - Số lượng video hiện tại đang quá hạn
     * - Thời gian timeout được cấu hình
     * - Thông tin về lần chạy scheduled job gần nhất
     *
     * @return ResponseEntity chứa thông tin trạng thái auto-reset system
     */
    @GetMapping("/auto-reset/status")
    public ResponseEntity<Map<String, Object>> getAutoResetStatus() {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to get auto-reset status", tenantId);
        
        try {
            long expiredVideoCount = videoAutoResetService.getExpiredVideoCount();
            List<Video> expiredVideos = videoAutoResetService.getExpiredVideos();
            
            Map<String, Object> response = createSuccessResponse(
                    "Lấy thông tin trạng thái auto-reset thành công",
                    Map.of(
                            "expiredVideoCount", expiredVideoCount,
                            "expiredVideos", expiredVideos.stream()
                                    .map(v -> Map.of(
                                            "id", v.getId(),
                                            "assignedStaff", v.getAssignedStaff(),
                                            "assignedAt", v.getAssignedAt(),
                                            "status", v.getStatus(),
                                            "customerName", v.getCustomerName()
                                    ))
                                    .toList(),
                            "systemStatus", "ACTIVE"
                    )
            );
            response.put("tenantId", tenantId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("[Tenant: {}] Error getting auto-reset status: ", tenantId, e);
            return createErrorResponse("Lỗi khi lấy thông tin auto-reset: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * POST /api/v1/videos/{id}/manual-reset - Reset manual một video cụ thể
     * 
     * API admin để reset manual video về trạng thái CHUA_AI_NHAN
     * Hữu ích cho testing hoặc xử lý exception cases
     *
     * @param id ID của video cần reset
     * @return ResponseEntity chứa kết quả reset
     */
    @PostMapping("/{id}/manual-reset")
    public ResponseEntity<Map<String, Object>> manualResetVideo(@PathVariable Long id) {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to manual reset video ID: {}", tenantId, id);
        
        try {
            boolean resetSuccess = videoAutoResetService.manualResetVideo(id);
            
            if (resetSuccess) {
                Map<String, Object> response = createSuccessResponse(
                        String.format("Video ID %d đã được reset thành công", id),
                        Map.of("videoId", id, "resetStatus", "SUCCESS")
                );
                response.put("tenantId", tenantId);
                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse(
                        String.format("Không thể reset video ID %d (có thể video không tồn tại hoặc chưa được assign)", id), 
                        HttpStatus.BAD_REQUEST);
            }
            
        } catch (Exception e) {
            log.error("[Tenant: {}] Error manual resetting video ID {}: ", tenantId, id, e);
            return createErrorResponse("Lỗi khi reset video: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/v1/videos/pricing-rules - Lấy tất cả pricing rules hiện tại
     * 
     * API utility để xem mapping giữa order_value và price
     * Hữu ích cho documentation, testing và admin dashboard
     *
     * @return ResponseEntity chứa tất cả pricing rules
     */
//    @GetMapping("/pricing-rules")
//    public ResponseEntity<Map<String, Object>> getPricingRules() {
//        String tenantId = TenantContext.getTenantId();
//        log.info("[Tenant: {}] Received request to get pricing rules", tenantId);
//
//        try {
//            Map<BigDecimal, BigDecimal> pricingRules = VideoPricingUtil.getAllPricingRules();
//
//            // Convert to more readable format
//            List<Map<String, Object>> rulesList = pricingRules.entrySet().stream()
//                    .map(entry -> {
//                        BigDecimal orderValue = entry.getKey();
//                        BigDecimal price = entry.getValue();
//                        BigDecimal margin = VideoPricingUtil.calculateProfitMargin(orderValue, price);
//
//                        return Map.of(
//                                "orderValue", orderValue,
//                                "price", price,
//                                "profitMargin", margin != null ? margin.toString() + "%" : "N/A",
//                                "description", String.format("Cost %s → Sell %s", orderValue, price)
//                        );
//                    })
//                    .sorted((a, b) -> ((BigDecimal) a.get("orderValue")).compareTo((BigDecimal) b.get("orderValue")))
//                    .toList();
//
//            Map<String, Object> response = createSuccessResponse(
//                    "Lấy pricing rules thành công",
//                    Map.of(
//                            "totalRules", pricingRules.size(),
//                            "rules", rulesList,
//                            "businessLogic", Map.of(
//                                    "description", "Pricing based on video duration and complexity",
//                                    "note", "Price is automatically calculated from order_value",
//                                    "lastUpdated", "2025-06-13"
//                            )
//                    )
//            );
//            response.put("tenantId", tenantId);
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("[Tenant: {}] Error getting pricing rules: ", tenantId, e);
//            return createErrorResponse("Lỗi khi lấy pricing rules: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }

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

    /**
     * GET /api/v1/videos/check-customer - Kiểm tra khách hàng đã tồn tại trong hệ thống
     * 
     * API này được sử dụng khi tạo mới video để cảnh báo nếu tên khách hàng đã tồn tại,
     * giúp tránh tình trạng trùng đơn hàng
     * 
     * @param customerName Tên khách hàng cần kiểm tra (required)
     * @return ResponseEntity chứa thông tin về việc khách hàng có tồn tại hay không
     *         - 200 OK: Trả về thông tin kiểm tra
     *         - 400 BAD_REQUEST: Tên khách hàng không hợp lệ
     */
    @GetMapping("/check-customer")
    public ResponseEntity<Map<String, Object>> checkCustomerExists(
            @RequestParam String customerName) {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to check customer existence: {}", tenantId, customerName);

        try {
            // Validate input
            if (customerName == null || customerName.trim().isEmpty()) {
                return createErrorResponse("Tên khách hàng không được để trống", HttpStatus.BAD_REQUEST);
            }

            String trimmedCustomerName = customerName.trim();
            if (trimmedCustomerName.length() < 2) {
                return createErrorResponse("Tên khách hàng phải có ít nhất 2 ký tự", HttpStatus.BAD_REQUEST);
            }

            // Kiểm tra khách hàng đã tồn tại
            boolean exists = videoService.checkCustomerExists(trimmedCustomerName);
            
            Map<String, Object> response = createSuccessResponse(
                    exists ? "Khách hàng đã tồn tại trong hệ thống" : "Khách hàng chưa tồn tại trong hệ thống",
                    Map.of(
                            "customerName", trimmedCustomerName,
                            "exists", exists,
                            "warning", exists ? 
                                String.format("Khách hàng '%s' đã tồn tại trong hệ thống, kiểm tra lại xem có thể bị trung đơn", trimmedCustomerName) : 
                                null
                    )
            );
            response.put("tenantId", tenantId);

            log.info("[Tenant: {}] Customer '{}' existence check result: {}", tenantId, trimmedCustomerName, exists);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error checking customer existence for '{}': ", tenantId, customerName, e);
            return createErrorResponse("Lỗi khi kiểm tra khách hàng: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}