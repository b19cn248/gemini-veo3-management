package com.ptit.google.veo3.controller;

import com.ptit.google.veo3.common.response.ApiResponse;
import com.ptit.google.veo3.common.response.PaginatedResponse;
import com.ptit.google.veo3.common.util.ResponseUtil;
import com.ptit.google.veo3.dto.*;
import com.ptit.google.veo3.entity.DeliveryStatus;
import com.ptit.google.veo3.entity.PaymentStatus;
import com.ptit.google.veo3.entity.Video;
import com.ptit.google.veo3.entity.VideoStatus;
import com.ptit.google.veo3.multitenant.TenantContext;
import com.ptit.google.veo3.service.VideoAutoResetService;
import com.ptit.google.veo3.service.VideoService;
import com.ptit.google.veo3.service.interfaces.IJwtTokenService;
import com.ptit.google.veo3.service.interfaces.IStaffWorkloadService;
import com.ptit.google.veo3.service.interfaces.IVideoPricingService;
import com.ptit.google.veo3.service.interfaces.IVideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
 * - PUT    /api/v1/videos/{id}  /api/v1/videos/{id}               - Cập nhật video
 * - DELETE /api/v1/videos/{id}               - Xóa video
 * - GET    /api/v1/videos/{id}               - Lấy chi tiết video
 * - GET    /api/v1/videos                    - Lấy danh sách video (có phân trang)
 * - GET    /api/v1/videos/all                - Lấy tất cả video (không phân trang)
 * - GET    /api/v1/videos/search             - Tìm kiếm video theo tên khách hàng
 * - GET    /api/v1/videos/search/id          - Tìm kiếm video theo ID
 * - GET    /api/v1/videos/status/{status}    - Lấy video theo trạng thái
 * - PATCH  /api/v1/videos/{id}/assigned-staff - Cập nhật nhân viên được giao (MAX: 3 videos)
 * - PATCH  /api/v1/videos/{id}/status        - Cập nhật trạng thái video (PERMISSION: chỉ assignedStaff)
 * - PATCH  /api/v1/videos/{id}/video-url     - Cập nhật link video (PERMISSION: chỉ assignedStaff)
 * - PUT    /api/v1/videos/{id}/delivery-status - Cập nhật trạng thái giao hàng
 * - PUT    /api/v1/videos/{id}/payment-status  - Cập nhật trạng thái thanh toán
 * - GET    /api/v1/videos/staff-workload     - Lấy thông tin workload của nhân viên
 * - GET    /api/v1/videos/sales-salaries    - Tính lương sales theo ngày thanh toán (NEW API)
 * - GET    /api/v1/videos/check-customer    - Kiểm tra khách hàng đã tồn tại (NEW API)
 * - POST   /api/v1/videos/staff-limit       - Thiết lập giới hạn nhân viên (ADMIN ONLY) - Max 3 đơn/ngày
 * - DELETE /api/v1/videos/staff-limit       - Hủy giới hạn nhân viên (ADMIN ONLY)
 * - GET    /api/v1/videos/staff-limits      - Lấy danh sách giới hạn đang active
 * - GET    /api/v1/videos/staff-limit/check - Kiểm tra quota hằng ngày của nhân viên
 * - GET    /api/v1/videos/{id}/customer-contact - Lấy thông tin liên hệ khách hàng theo ID (NEW API)
 */
@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Cho phép CORS từ mọi origin (production nên hạn chế)
public class VideoController {

    private final IVideoService videoService;
    private final IJwtTokenService jwtTokenService;
    private final IStaffWorkloadService staffWorkloadService;
    private final VideoAutoResetService videoAutoResetService;
    private final IVideoPricingService videoPricingService;

    /**
     * POST /api/v1/videos - Tạo mới video
     *
     * @param requestDto - Dữ liệu video cần tạo (đã được validate)
     * @return ResponseEntity chứa thông tin video vừa tạo hoặc thông báo lỗi
     */
    @PostMapping
    public ResponseEntity<ApiResponse<VideoResponseDto>> createVideo(
            @Valid @RequestBody VideoRequestDto requestDto
    ) {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to create video for customer: {}", tenantId, requestDto.getCustomerName());

        Integer videoDuration = requestDto.getVideoDuration();

        // AUTO-CALCULATE ORDER VALUE AND PRICE: Using VideoPricingService
        BigDecimal[] orderValueAndPrice = videoPricingService.calculateOrderValueAndPrice(videoDuration);
        BigDecimal orderValue = orderValueAndPrice[0];
        BigDecimal calculatedPrice = orderValueAndPrice[1];

        requestDto.setOrderValue(orderValue);

        if (calculatedPrice != null) {
            requestDto.setPrice(calculatedPrice);
            log.info("[Tenant: {}] Auto-calculated price {} for order value {} (customer: {})",
                    tenantId, calculatedPrice, orderValue, requestDto.getCustomerName());
        } else {
            log.warn("[Tenant: {}] No pricing rule found for order value {} (customer: {})",
                    tenantId, orderValue, requestDto.getCustomerName());
        }

        VideoResponseDto createdVideo = videoService.createVideo(requestDto);

        log.info("[Tenant: {}] Video created successfully with ID: {}", tenantId, createdVideo.getId());
        return ResponseUtil.created("Video được tạo thành công", createdVideo);
    }

    /**
     * PUT /api/v1/videos/{id} - Cập nhật video
     *
     * @param id         - ID của video cần cập nhật
     * @param requestDto - Dữ liệu mới để cập nhật (đã được validate)
     * @return ResponseEntity chứa thông tin video sau khi cập nhật hoặc thông báo lỗi
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoResponseDto>> updateVideo(
            @PathVariable Long id,
            @Valid @RequestBody VideoRequestDto requestDto) {

        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to update video with ID: {}", tenantId, id);

        Integer videoDuration = requestDto.getVideoDuration();

        // AUTO-CALCULATE ORDER VALUE AND PRICE: Using VideoPricingService
        BigDecimal[] orderValueAndPrice = videoPricingService.calculateOrderValueAndPrice(videoDuration);
        BigDecimal orderValue = orderValueAndPrice[0];
        BigDecimal calculatedPrice = orderValueAndPrice[1];

        requestDto.setOrderValue(orderValue);

        if (calculatedPrice != null) {
            requestDto.setPrice(calculatedPrice);
            log.info("[Tenant: {}] Auto-calculated price {} for order value {} during update (video ID: {})",
                    tenantId, calculatedPrice, orderValue, id);
        } else {
            log.warn("[Tenant: {}] No pricing rule found for order value {} during update (video ID: {})",
                    tenantId, orderValue, id);
        }

        VideoResponseDto updatedVideo = videoService.updateVideo(id, requestDto);

        log.info("[Tenant: {}] Video updated successfully with ID: {}", tenantId, id);
        return ResponseUtil.ok("Video được cập nhật thành công", updatedVideo);
    }

    /**
     * PATCH /api/v1/videos/{id}/assigned-staff - Cập nhật nhân viên được giao
     *
     * @param id            - ID của video cần cập nhật
     * @param assignedStaff - Tên nhân viên được giao mới
     * @return ResponseEntity chứa thông tin video sau khi cập nhật hoặc thông báo lỗi
     */
    @PatchMapping("/{id}/assigned-staff")
    public ResponseEntity<ApiResponse<VideoResponseDto>> updateAssignedStaff(
            @PathVariable Long id,
            @RequestParam String assignedStaff) {

        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to update assigned staff for video ID: {} to staff: {}",
                tenantId, id, assignedStaff);

        VideoResponseDto updatedVideo = videoService.updateAssignedStaff(id, assignedStaff);
        return ResponseUtil.ok("Nhân viên được giao đã được cập nhật thành công", updatedVideo);
    }

    /**
     * PATCH Cập nhật trạng thái video - Cập nhật trạng thái video
     * <p>
     * SECURITY: Chỉ có nhân viên được giao video mới có quyền cập nhật trạng thái
     * Logic phân quyền: So sánh trường "name" từ JWT token với assignedStaff của video
     *
     * @param id     - ID của video cần cập nhật
     * @param status - Trạng thái mới của video
     * @return ResponseEntity chứa thông tin video sau khi cập nhật hoặc thông báo lỗi
     * - 200 OK: Cập nhật thành công
     * - 403 FORBIDDEN: Không có quyền cập nhật (không phải người được giao)
     * - 404 NOT_FOUND: Không tìm thấy video
     * - 400 BAD_REQUEST: Tham số không hợp lệ
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<VideoResponseDto>> updateVideoStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to update status for video ID: {} to status: {}",
                tenantId, id, status);

        VideoResponseDto updatedVideo = videoService.updateVideoStatus(id, status);
        return ResponseUtil.ok("Trạng thái video đã được cập nhật thành công", updatedVideo);
    }

    /**
     * PATCH /api/v1/videos/{id}/video-url - Cập nhật link video
     * <p>
     * SECURITY: Chỉ có nhân viên được giao video mới có quyền cập nhật video URL
     * Logic phân quyền: So sánh trường "name" từ JWT token với assignedStaff của video
     *
     * @param id       - ID của video cần cập nhật
     * @param videoUrl - Link video mới
     * @return ResponseEntity chứa thông tin video sau khi cập nhật hoặc thông báo lỗi
     * - 200 OK: Cập nhật thành công
     * - 403 FORBIDDEN: Không có quyền cập nhật (không phải người được giao)
     * - 404 NOT_FOUND: Không tìm thấy video
     * - 400 BAD_REQUEST: Tham số không hợp lệ
     */
    @PatchMapping("/{id}/video-url")
    public ResponseEntity<ApiResponse<VideoResponseDto>> updateVideoUrl(
            @PathVariable Long id,
            @RequestParam String videoUrl) {

        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to update video URL for video ID: {} to URL: {}",
                tenantId, id, videoUrl);

        VideoResponseDto updatedVideo = videoService.updateVideoUrl(id, videoUrl);
        return ResponseUtil.ok("Link video đã được cập nhật thành công", updatedVideo);
    }

    /**
     * POST /api/v1/videos/{id}/cancel - Hủy video (ADMIN ONLY)
     * <p>
     * Reset video về trạng thái CHUA_AI_NHAN và assignedStaff = null
     * <p>
     * SECURITY: Chỉ có admin mới có quyền sử dụng API này
     * BUSINESS LOGIC: Reset video về trạng thái ban đầu
     *
     * @param id ID của video cần hủy
     * @return ResponseEntity chứa thông tin video sau khi hủy hoặc thông báo lỗi
     * - 200 OK: Hủy thành công
     * - 403 FORBIDDEN: Không phải admin
     * - 404 NOT_FOUND: Không tìm thấy video
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<VideoResponseDto>> cancelVideo(@PathVariable Long id) {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to cancel video ID: {}", tenantId, id);

        VideoResponseDto canceledVideo = videoService.cancelVideo(id);
        return ResponseUtil.ok("Video đã được hủy thành công", canceledVideo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteVideo(@PathVariable Long id) {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to delete video with ID: {}", tenantId, id);

        videoService.deleteVideo(id);
        return ResponseUtil.ok("Video được xóa thành công");
    }

    /**
     * GET /api/v1/videos/{id} - Lấy thông tin chi tiết video
     *
     * @param id - ID của video cần lấy thông tin
     * @return ResponseEntity chứa thông tin video hoặc thông báo lỗi
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoResponseDto>> getVideoById(@PathVariable Long id) {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to get video with ID: {}", tenantId, id);

        VideoResponseDto video = videoService.getVideoById(id);
        video.setBillImageUrl(null);
        return ResponseUtil.ok("Lấy thông tin video thành công", video);
    }

    /**
     * GET /api/v1/videos - Lấy danh sách tất cả video (có phân trang)
     * <p>
     * Query Parameters:
     * - page: Số trang (mặc định: 0)
     * - size: Kích thước trang (mặc định: 10)
     * - status: Lọc theo trạng thái video (tùy chọn)
     * - assignedStaff: Lọc theo nhân viên được giao (tùy chọn)
     * - deliveryStatus: Lọc theo trạng thái giao hàng (tùy chọn)
     * - paymentStatus: Lọc theo trạng thái thanh toán (tùy chọn)
     * - fromPaymentDate: Lọc từ ngày thanh toán (tùy chọn, định dạng: yyyy-MM-dd)
     * - toPaymentDate: Lọc đến ngày thanh toán (tùy chọn, định dạng: yyyy-MM-dd)
     * - fromDateCreatedVideo: Lọc từ ngày tạo video (tùy chọn, định dạng: yyyy-MM-dd)
     * - toDateCreatedVideo: Lọc đến ngày tạo video (tùy chọn, định dạng: yyyy-MM-dd)
     * - createdBy: Lọc theo người tạo (tùy chọn)
     * - sortBy: Trường để sắp xếp (mặc định: createdAt)
     * - sortDirection: Hướng sắp xếp - asc/desc (mặc định: desc)
     * <p>
     * Lưu ý về lọc theo khoảng thời gian:
     * - Nếu chỉ có fromDate: Lấy từ ngày đó trở đi
     * - Nếu chỉ có toDate: Lấy đến ngày đó
     * - Nếu có cả hai: Lấy trong khoảng từ fromDate đến toDate
     * - Nếu fromDate = toDate: Lấy tất cả video trong ngày đó
     * - Nếu không có: Lấy tất cả video (mặc định)
     *
     * @param page                   - Số trang cần lấy
     * @param size                   - Số lượng record trên mỗi trang
     * @param status                 - Trạng thái video cần lọc
     * @param assignedStaff          - Nhân viên được giao cần lọc
     * @param deliveryStatus         - Trạng thái giao hàng cần lọc
     * @param paymentStatus          - Trạng thái thanh toán cần lọc
     * @param fromPaymentDate        - Lọc từ ngày thanh toán
     * @param toPaymentDate          - Lọc đến ngày thanh toán
     * @param fromDateCreatedVideo   - Lọc từ ngày tạo video
     * @param toDateCreatedVideo     - Lọc đến ngày tạo video
     * @param createdBy              - Người tạo video cần lọc
     * @param sortBy                 - Trường để sắp xếp
     * @param sortDirection          - Hướng sắp xếp (asc hoặc desc)
     * @return ResponseEntity chứa danh sách video với thông tin phân trang
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<VideoResponseDto>> getAllVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) VideoStatus status,
            @RequestParam(required = false) String assignedStaff,
            @RequestParam(required = false) DeliveryStatus deliveryStatus,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromPaymentDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toPaymentDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDateCreatedVideo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDateCreatedVideo,
            @RequestParam(required = false) String createdBy,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {

        String tenantId = TenantContext.getTenantId();

        log.info("[Tenant: {}] Received request to get all videos - page: {}, size: {}, sortBy: {}, direction: {}",
                tenantId, page, size, sortBy, sortDirection);

        Page<VideoResponseDto> videoPage = videoService.getAllVideos(page, size, sortBy, sortDirection,
                status, assignedStaff, deliveryStatus, paymentStatus, fromPaymentDate, toPaymentDate,
                fromDateCreatedVideo, toDateCreatedVideo, createdBy);

        // Apply billImageUrl permission logic to all videos in the page
        applyBillImageUrlPermissions(videoPage.getContent());

        return ResponseUtil.okPaginated("Lấy danh sách video thành công", videoPage);
    }

    /**
     * GET /api/v1/videos/all - Lấy tất cả video không phân trang
     * Thích hợp để lấy toàn bộ dữ liệu cho export hoặc các use case khác
     *
     * @return ResponseEntity chứa toàn bộ danh sách video
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<VideoResponseDto>>> getAllVideosWithoutPagination() {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to get all videos without pagination", tenantId);

        List<VideoResponseDto> videos = videoService.getAllVideos();
        
        // Apply billImageUrl permission logic
        applyBillImageUrlPermissions(videos);
        
        return ResponseUtil.ok("Lấy danh sách video thành công", videos);
    }

    /**
     * GET /api/v1/videos/search - Tìm kiếm video theo tên khách hàng
     *
     * @param customerName - Tên khách hàng cần tìm (tìm kiếm gần đúng, không phân biệt hoa thường)
     * @return ResponseEntity chứa danh sách video tìm được
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<VideoResponseDto>>> searchVideosByCustomerName(
            @RequestParam String customerName
    ) {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to search videos by customer name: {}", tenantId, customerName);

        List<VideoResponseDto> videos = videoService.searchByCustomerName(customerName);
        
        // Apply billImageUrl permission logic
        applyBillImageUrlPermissions(videos);
        
        return ResponseUtil.ok(
                String.format("Tìm thấy %d video cho khách hàng '%s'", videos.size(), customerName),
                videos
        );
    }

    /**
     * GET /api/v1/videos/search/id - Tìm kiếm video theo ID
     *
     * @param id - ID của video cần tìm
     * @return ResponseEntity chứa video tìm được hoặc danh sách rỗng
     */
    @GetMapping("/search/id")
    public ResponseEntity<ApiResponse<List<VideoResponseDto>>> searchVideoById(
            @RequestParam Long id
    ) {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to search video by ID: {}", tenantId, id);

        List<VideoResponseDto> videos = videoService.searchById(id);
        
        // Apply billImageUrl permission logic
        applyBillImageUrlPermissions(videos);
        
        String message = videos.isEmpty() ?
                String.format("Không tìm thấy video với ID '%d'", id) :
                String.format("Tìm thấy video với ID '%d'", id);

        return ResponseUtil.ok(message, videos);
    }

    /**
     * GET /api/v1/videos/status/{status} - Lấy danh sách video theo trạng thái
     *
     * @param status - Trạng thái video cần lọc
     * @return ResponseEntity chứa danh sách video có trạng thái tương ứng
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<VideoResponseDto>>> getVideosByStatus(@PathVariable String status) {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to get videos by status: {}", tenantId, status);

        List<VideoResponseDto> videos = videoService.getVideosByStatus(status);
        
        // Apply billImageUrl permission logic
        applyBillImageUrlPermissions(videos);
        
        return ResponseUtil.ok(
                String.format("Lấy danh sách video có trạng thái '%s' thành công", status),
                videos
        );
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
    public ResponseEntity<ApiResponse<List<VideoResponseDto>>> filterVideos(
            @RequestParam(required = false) String assignedStaff,
            @RequestParam(required = false) String deliveryStatus,
            @RequestParam(required = false) String paymentStatus) {

        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to filter videos - staff: {}, delivery status: {}, payment status: {}",
                tenantId, assignedStaff, deliveryStatus, paymentStatus);

        List<VideoResponseDto> videos = videoService.filterVideos(assignedStaff, deliveryStatus, paymentStatus);
        
        // Apply billImageUrl permission logic
        applyBillImageUrlPermissions(videos);
        
        return ResponseUtil.ok("Lọc video thành công", videos);
    }

    /**
     * PUT /api/v1/videos/{id}/delivery-status - Cập nhật trạng thái giao hàng
     *
     * @param id     - ID của video cần cập nhật
     * @param status - Trạng thái giao hàng mới
     * @return ResponseEntity chứa thông tin video sau khi cập nhật hoặc thông báo lỗi
     */
    @PutMapping("/{id}/delivery-status")
    public ResponseEntity<ApiResponse<VideoResponseDto>> updateDeliveryStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to update delivery status for video ID: {} to status: {}",
                tenantId, id, status);

        try {
            VideoResponseDto updatedVideo = videoService.updateDeliveryStatus(id, status);
            return ResponseUtil.ok("Trạng thái giao hàng đã được cập nhật thành công", updatedVideo);

        } catch (VideoService.VideoNotFoundException e) {
            log.warn("[Tenant: {}] Video not found with ID: {}", tenantId, id);
            return ResponseUtil.notFound(e.getMessage());

        } catch (IllegalArgumentException e) {
            log.warn("[Tenant: {}] Invalid delivery status for video ID {}: {}", tenantId, id, e.getMessage());
            return ResponseUtil.badRequest(e.getMessage());

        } catch (Exception e) {
            log.error("[Tenant: {}] Error updating delivery status for video ID {}: ", tenantId, id, e);
            return ResponseUtil.internalServerError("Lỗi khi cập nhật trạng thái giao hàng: " + e.getMessage());
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
    public ResponseEntity<ApiResponse<VideoResponseDto>> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to update payment status for video ID: {} to status: {}",
                tenantId, id, status);

        try {
            VideoResponseDto updatedVideo = videoService.updatePaymentStatus(id, status);
            return ResponseUtil.ok("Trạng thái thanh toán đã được cập nhật thành công", updatedVideo);

        } catch (VideoService.VideoNotFoundException e) {
            log.warn("[Tenant: {}] Video not found with ID: {}", tenantId, id);
            return ResponseUtil.notFound(e.getMessage());

        } catch (IllegalArgumentException e) {
            log.warn("[Tenant: {}] Invalid payment status for video ID {}: {}", tenantId, id, e.getMessage());
            return ResponseUtil.badRequest(e.getMessage());

        } catch (Exception e) {
            log.error("[Tenant: {}] Error updating payment status for video ID {}: ", tenantId, id, e);
            return ResponseUtil.internalServerError("Lỗi khi cập nhật trạng thái thanh toán: " + e.getMessage());
        }
    }

    /**
     * PUT /api/v1/videos/{id}/bill-image-url - Cập nhật URL hình ảnh hóa đơn
     * <p>
     * Chỉ người tạo video mới có quyền cập nhật hình ảnh hóa đơn.
     *
     * @param id           - ID của video cần cập nhật
     * @param billImageUrl - URL hình ảnh hóa đơn mới
     * @return ResponseEntity chứa thông tin video đã cập nhật
     */
    @PutMapping("/{id}/bill-image-url")
    public ResponseEntity<ApiResponse<VideoResponseDto>> updateBillImageUrl(
            @PathVariable Long id,
            @RequestParam String billImageUrl) {
        
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to update bill image URL for video ID: {} to: {}", 
                tenantId, id, billImageUrl);
        
        try {
            VideoResponseDto updatedVideo = videoService.updateBillImageUrl(id, billImageUrl);
            return ResponseUtil.ok("URL hình ảnh hóa đơn đã được cập nhật thành công", updatedVideo);
        } catch (VideoService.VideoNotFoundException e) {
            log.warn("[Tenant: {}] Video not found for ID {}: {}", tenantId, id, e.getMessage());
            return ResponseUtil.notFound(e.getMessage());
        } catch (SecurityException e) {
            log.warn("[Tenant: {}] Security violation updating bill image URL for video ID {}: {}", 
                    tenantId, id, e.getMessage());
            return ResponseUtil.forbidden(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("[Tenant: {}] Invalid request for video ID {}: {}", tenantId, id, e.getMessage());
            return ResponseUtil.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("[Tenant: {}] Error updating bill image URL for video ID {}: ", tenantId, id, e);
            return ResponseUtil.internalServerError("Lỗi khi cập nhật URL hình ảnh hóa đơn: " + e.getMessage());
        }
    }

    /**
     * GET /api/v1/videos/assigned-staff - Lấy danh sách các nhân viên được giao khác nhau
     *
     * @return ResponseEntity chứa danh sách tên nhân viên
     */
    @GetMapping("/assigned-staff")
    public ResponseEntity<ApiResponse<List<String>>> getDistinctAssignedStaff() {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to get distinct assigned staff names", tenantId);

        List<String> staffList = videoService.getDistinctAssignedStaff();

        List<String> staffNames = new ArrayList<>();

        for (String staff : staffList) {
            if (staff.isBlank()) {
                staffNames.add("Chưa ai nhận");
            } else {
                staffNames.add(staff);
            }
        }

        return ResponseUtil.ok("Lấy danh sách nhân viên thành công", staffNames);
    }

    /**
     * GET /api/v1/videos/creators - Lấy danh sách các người tạo video khác nhau
     * Trả về danh sách distinct createdBy từ bảng Video
     *
     * @return ResponseEntity chứa danh sách tên người tạo video
     */
    @GetMapping("/creators")
    public ResponseEntity<ApiResponse<List<String>>> getDistinctCreatedBy() {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to get distinct video creators", tenantId);

        List<String> creatorList = videoService.getDistinctCreatedBy();
        return ResponseUtil.ok("Lấy danh sách người tạo video thành công", creatorList);
    }

    /**
     * GET /api/v1/videos/staff-salaries - Lấy tổng tiền lương của các nhân viên theo khoảng thời gian
     * Tính các video đã thanh toán và bị bùng
     * Lọc theo khoảng thời gian thanh toán
     *
     * @param startDate Ngày bắt đầu thống kê (format: yyyy-MM-dd, required)
     * @param endDate Ngày kết thúc thống kê (format: yyyy-MM-dd, required)
     * @return ResponseEntity chứa danh sách tổng tiền lương của từng nhân viên
     */
    @GetMapping("/staff-salaries")
    public ResponseEntity<ApiResponse<List<StaffSalaryDto>>> getStaffSalariesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to get staff salaries from {} to {}", tenantId, startDate, endDate);

        List<StaffSalaryDto> salaries = videoService.calculateStaffSalariesByDateRange(startDate, endDate);
        String message = startDate.equals(endDate) ?
                String.format("Lấy thông tin lương nhân viên ngày %s thành công (bao gồm đơn bị bùng)", startDate) :
                String.format("Lấy thông tin lương nhân viên từ %s đến %s thành công (bao gồm đơn bị bùng)", startDate, endDate);

        return ResponseUtil.ok(message, salaries);
    }

    /**
     * GET /api/v1/videos/sales-salaries - Tính lương sales với realm super admin access control
     * <p>
     * Business Logic:
     * - Super Admin (realm role "admin"): Xem lương của TẤT CẢ sales trong hệ thống
     * - Regular User (no realm admin): Chỉ xem lương cá nhân
     * - Lọc videos có paymentStatus = 'DA_THANH_TOAN'
     * - Lọc theo paymentDate trong khoảng startDate đến endDate
     * - Hoa hồng = tổng price * 10% hoặc 12% (tùy sales)
     * 
     * Role-Based Access:
     * - Super Admin (realm_access.roles[] contains "admin"): calculateSalesSalariesByDateRange() - tất cả sales
     * - Regular User: calculateSalesSalariesByDateRangeForCurrentUser() - chỉ mình
     * 
     * Note: Chỉ realm admin mới có quyền xem tất cả, không phải resource admin
     *
     * @param startDate Ngày bắt đầu thống kê (format: yyyy-MM-dd, required)
     * @param endDate Ngày kết thúc thống kê (format: yyyy-MM-dd, required)
     * @return ResponseEntity chứa thông tin lương sales (tất cả cho super admin, cá nhân cho user)
     */
    @GetMapping("/sales-salaries")
    public ResponseEntity<ApiResponse<List<SalesSalaryDto>>> getSalesSalariesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String tenantId = TenantContext.getTenantId();
        
        // Lấy thông tin user hiện tại và check realm super admin role
        String currentSalesName;
        boolean isSuperAdmin;
        try {
            currentSalesName = jwtTokenService.getCurrentUserNameFromJwt();
            isSuperAdmin = jwtTokenService.isCurrentUserAdmin(); // ✅ REALM role check
        } catch (Exception e) {
            log.error("[Tenant: {}] Failed to extract user info from JWT: {}", tenantId, e.getMessage());
            throw new SecurityException("Không thể xác định thông tin người dùng từ token");
        }
        
        List<SalesSalaryDto> salesSalaries;
        String message;
        
        if (isSuperAdmin) {
            // ✅ SUPER ADMIN (realm role "admin"): Xem tất cả sales
            log.info("[Tenant: {}] Super Admin '{}' (realm role) calculating ALL sales salaries from {} to {}", 
                    tenantId, currentSalesName, startDate, endDate);
            
            salesSalaries = videoService.calculateSalesSalariesByDateRange(startDate, endDate);
            
            message = startDate.equals(endDate) ?
                    String.format("Super Admin - Tính lương tất cả sales ngày %s thành công", startDate) :
                    String.format("Super Admin - Tính lương tất cả sales từ %s đến %s thành công", startDate, endDate);
                    
        } else {
            // ✅ REGULAR USER (no realm admin): Chỉ xem lương cá nhân
            log.info("[Tenant: {}] User '{}' calculating personal sales salaries from {} to {}", 
                    tenantId, currentSalesName, startDate, endDate);
            
            salesSalaries = videoService.calculateSalesSalariesByDateRangeForCurrentUser(
                    startDate, endDate, currentSalesName);
            
            message = startDate.equals(endDate) ?
                    String.format("Tính lương cá nhân ngày %s thành công", startDate) :
                    String.format("Tính lương cá nhân từ %s đến %s thành công", startDate, endDate);
        }
        
        // Enhanced audit logging với super admin info
        log.info("[Tenant: {}] Sales salary calculation completed for user '{}' (super_admin: {}) - {} records returned", 
                tenantId, currentSalesName, isSuperAdmin, salesSalaries.size());
        
        return ResponseUtil.ok(message, salesSalaries);
    }

    /**
     * GET /api/v1/videos/staff-workload - Lấy thông tin workload của nhân viên
     * <p>
     * API mới để monitoring và debugging workload của từng nhân viên
     * Hữu ích cho việc theo dõi và quản lý phân bổ công việc
     *
     * @param staffName Tên nhân viên cần kiểm tra (optional - nếu không có sẽ lấy từ JWT)
     * @return ResponseEntity chứa thông tin workload chi tiết
     */
    @GetMapping("/staff-workload")
    public ResponseEntity<ApiResponse<WorkloadInfo>> getStaffWorkload(
            @RequestParam(required = false) String staffName) {
        String tenantId = TenantContext.getTenantId();

        // Nếu không có staffName thì lấy từ JWT của user hiện tại
        String targetStaff = staffName;
        if (!StringUtils.hasText(targetStaff)) {
            targetStaff = jwtTokenService.getCurrentUserNameFromJwt();
        }

        log.info("[Tenant: {}] Received request to get workload for staff: {}", tenantId, targetStaff);

        WorkloadInfo workloadInfo = staffWorkloadService.getWorkloadInfo(targetStaff);
        return ResponseUtil.ok(
                String.format("Lấy thông tin workload của nhân viên '%s' thành công", targetStaff),
                workloadInfo
        );
    }

    /**
     * GET /api/v1/videos/auto-reset/status - Lấy thông tin trạng thái auto-reset system
     * <p>
     * API monitoring để kiểm tra:
     * - Số lượng video hiện tại đang quá hạn
     * - Thời gian timeout được cấu hình
     * - Thông tin về lần chạy scheduled job gần nhất
     *
     * @return ResponseEntity chứa thông tin trạng thái auto-reset system
     */
    @GetMapping("/auto-reset/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAutoResetStatus() {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to get auto-reset status", tenantId);

        try {
            long expiredVideoCount = videoAutoResetService.getExpiredVideoCount();
            List<Video> expiredVideos = videoAutoResetService.getExpiredVideos();

            Map<String, Object> responseData = Map.of(
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
            );

            return ResponseUtil.ok("Lấy thông tin trạng thái auto-reset thành công", responseData);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error getting auto-reset status: ", tenantId, e);
            return ResponseUtil.internalServerError("Lỗi khi lấy thông tin auto-reset: " + e.getMessage());
        }
    }

    /**
     * POST /api/v1/videos/{id}/manual-reset - Reset manual một video cụ thể
     * <p>
     * API admin để reset manual video về trạng thái CHUA_AI_NHAN
     * Hữu ích cho testing hoặc xử lý exception cases
     *
     * @param id ID của video cần reset
     * @return ResponseEntity chứa kết quả reset
     */
    @PostMapping("/{id}/manual-reset")
    public ResponseEntity<ApiResponse<Map<String, Object>>> manualResetVideo(@PathVariable Long id) {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to manual reset video ID: {}", tenantId, id);

        try {
            boolean resetSuccess = videoAutoResetService.manualResetVideo(id);

            if (resetSuccess) {
                Map<String, Object> responseData = Map.of("videoId", id, "resetStatus", "SUCCESS");
                return ResponseUtil.ok(String.format("Video ID %d đã được reset thành công", id), responseData);
            } else {
                return ResponseUtil.badRequest(
                        String.format("Không thể reset video ID %d (có thể video không tồn tại hoặc chưa được assign)", id));
            }

        } catch (Exception e) {
            log.error("[Tenant: {}] Error manual resetting video ID {}: ", tenantId, id, e);
            return ResponseUtil.internalServerError("Lỗi khi reset video: " + e.getMessage());
        }
    }


    /**
     * GET /api/v1/videos/check-customer - Kiểm tra khách hàng đã tồn tại trong hệ thống
     * <p>
     * API này được sử dụng khi tạo mới video để cảnh báo nếu tên khách hàng đã tồn tại,
     * giúp tránh tình trạng trùng đơn hàng
     *
     * @param customerName Tên khách hàng cần kiểm tra (required)
     * @return ResponseEntity chứa thông tin về việc khách hàng có tồn tại hay không
     * - 200 OK: Trả về thông tin kiểm tra
     * - 400 BAD_REQUEST: Tên khách hàng không hợp lệ
     */
    @GetMapping("/check-customer")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkCustomerExists(
            @RequestParam String customerName) {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to check customer existence: {}", tenantId, customerName);

        // Validate input
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên khách hàng không được để trống");
        }

        String trimmedCustomerName = customerName.trim();
        if (trimmedCustomerName.length() < 2) {
            throw new IllegalArgumentException("Tên khách hàng phải có ít nhất 2 ký tự");
        }

        // Kiểm tra khách hàng đã tồn tại
        boolean exists = videoService.checkCustomerExists(trimmedCustomerName);

        Map<String, Object> data = Map.of(
                "customerName", trimmedCustomerName,
                "exists", exists,
                "warning", exists ?
                        String.format("Khách hàng '%s' đã tồn tại trong hệ thống, kiểm tra lại xem có thể bị trung đơn", trimmedCustomerName) :
                        null
        );

        String message = exists ? "Khách hàng đã tồn tại trong hệ thống" : "Khách hàng chưa tồn tại trong hệ thống";

        log.info("[Tenant: {}] Customer '{}' existence check result: {}", tenantId, trimmedCustomerName, exists);
        return ResponseUtil.ok(message, data);
    }

    /**
     * POST /api/v1/videos/staff-limit - Thiết lập giới hạn cho nhân viên (ADMIN ONLY)
     *
     * @param staffName       Tên nhân viên cần giới hạn (required)
     * @param lockDays        Số ngày khóa (required, max 30 ngày)
     * @param maxOrdersPerDay Số đơn tối đa có thể nhận trong một ngày (optional, default 3, min 1, max 50)
     * @return ResponseEntity chứa thông tin giới hạn đã tạo
     * - 200 OK: Tạo giới hạn thành công
     * - 400 BAD_REQUEST: Tham số không hợp lệ
     * - 403 FORBIDDEN: Không phải admin
     */
    @PostMapping("/staff-limit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setStaffLimit(
            @RequestParam String staffName,
            @RequestParam Integer lockDays,
            @RequestParam(required = false, defaultValue = "3") Integer maxOrdersPerDay
    ) {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to set staff limit - Staff: '{}', Days: {}, Max orders per day: {}",
                tenantId, staffName, lockDays, maxOrdersPerDay);

        try {
            // Validate maxOrdersPerDay
            if (maxOrdersPerDay < 1 || maxOrdersPerDay > 50) {
                return ResponseUtil.badRequest("Số đơn tối đa trong ngày phải từ 1 đến 50");
            }

            videoService.setStaffLimit(staffName, lockDays, maxOrdersPerDay);

            // Lấy thông tin limit vừa tạo để trả về
            Map<String, Object> limitInfo = new HashMap<>();
            limitInfo.put("staffName", staffName.trim());
            limitInfo.put("lockDays", lockDays);
            limitInfo.put("maxOrdersPerDay", maxOrdersPerDay);
            limitInfo.put("startDate", LocalDateTime.now());
            limitInfo.put("endDate", LocalDateTime.now().plusDays(lockDays));
            limitInfo.put("remainingDays", lockDays);
            limitInfo.put("createdBy", jwtTokenService.getCurrentUserNameFromJwt());

            String message = String.format("Đã thiết lập giới hạn cho nhân viên '%s' trong %d ngày với tối đa %d đơn/ngày",
                    staffName.trim(), lockDays, maxOrdersPerDay);

            return ResponseUtil.ok(message, limitInfo);

        } catch (IllegalArgumentException e) {
            log.warn("[Tenant: {}] Invalid parameters for staff limit: {}", tenantId, e.getMessage());
            return ResponseUtil.badRequest(e.getMessage());

        } catch (SecurityException e) {
            log.warn("[Tenant: {}] Access denied for staff limit creation: {}", tenantId, e.getMessage());
            return ResponseUtil.forbidden("Chỉ admin mới có quyền thiết lập giới hạn nhân viên");

        } catch (Exception e) {
            log.error("[Tenant: {}] Error setting staff limit for '{}': ", tenantId, staffName, e);
            return ResponseUtil.internalServerError("Lỗi khi thiết lập giới hạn nhân viên: " + e.getMessage());
        }
    }

    /**
     * DELETE /api/v1/videos/staff-limit - Hủy giới hạn nhân viên (ADMIN ONLY)
     *
     * @param staffName Tên nhân viên cần hủy giới hạn (required)
     * @return ResponseEntity chứa kết quả hủy giới hạn
     * - 200 OK: Hủy giới hạn thành công
     * - 400 BAD_REQUEST: Nhân viên không có giới hạn
     * - 403 FORBIDDEN: Không phải admin
     */
    @DeleteMapping("/staff-limit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeStaffLimit(
            @RequestParam String staffName) {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to remove staff limit for: '{}'", tenantId, staffName);

        try {
            videoService.removeStaffLimit(staffName);

            Map<String, Object> responseData = Map.of("staffName", staffName.trim(), "action", "LIMIT_REMOVED");
            String message = String.format("Đã hủy giới hạn cho nhân viên '%s'", staffName.trim());

            return ResponseUtil.ok(message, responseData);

        } catch (IllegalArgumentException e) {
            log.warn("[Tenant: {}] Cannot remove staff limit: {}", tenantId, e.getMessage());
            return ResponseUtil.badRequest(e.getMessage());

        } catch (SecurityException e) {
            log.warn("[Tenant: {}] Access denied for staff limit removal: {}", tenantId, e.getMessage());
            return ResponseUtil.forbidden("Chỉ admin mới có quyền hủy giới hạn nhân viên");

        } catch (Exception e) {
            log.error("[Tenant: {}] Error removing staff limit for '{}': ", tenantId, staffName, e);
            return ResponseUtil.internalServerError("Lỗi khi hủy giới hạn nhân viên: " + e.getMessage());
        }
    }

    /**
     * GET /api/v1/videos/staff-limits - Lấy danh sách giới hạn nhân viên đang active
     *
     * @return ResponseEntity chứa danh sách tất cả giới hạn đang có hiệu lực
     * - 200 OK: Lấy danh sách thành công
     */
    @GetMapping("/staff-limits")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveStaffLimits() {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to get active staff limits", tenantId);

        try {
            List<Map<String, Object>> activeLimits = videoService.getActiveStaffLimits();

            Map<String, Object> responseData = Map.of(
                    "limits", activeLimits,
                    "total", activeLimits.size()
            );

            String message = String.format("Lấy danh sách giới hạn nhân viên thành công - %d giới hạn đang có hiệu lực", activeLimits.size());
            return ResponseUtil.ok(message, responseData);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error getting active staff limits: ", tenantId, e);
            return ResponseUtil.internalServerError("Lỗi khi lấy danh sách giới hạn nhân viên: " + e.getMessage());
        }
    }

    /**
     * GET /api/v1/videos/staff-limit/check - Kiểm tra quota và trạng thái giới hạn nhân viên
     *
     * @param staffName Tên nhân viên cần kiểm tra (required)
     * @return ResponseEntity chứa thông tin chi tiết về quota và trạng thái giới hạn
     * - 200 OK: Kiểm tra thành công
     */
    @GetMapping("/staff-limit/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkStaffLimit(
            @RequestParam String staffName) {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to check staff quota for: '{}'", tenantId, staffName);

        try {
            Map<String, Object> quotaInfo = videoService.getStaffQuotaInfo(staffName);
            String message = (String) quotaInfo.get("message");

            return ResponseUtil.ok(message, quotaInfo);

        } catch (Exception e) {
            log.error("[Tenant: {}] Error checking staff quota for '{}': ", tenantId, staffName, e);
            return ResponseUtil.internalServerError("Lỗi khi kiểm tra quota nhân viên: " + e.getMessage());
        }
    }

    /**
     * GET /api/v1/videos/{id}/customer-contact - Lấy thông tin liên hệ khách hàng theo ID
     * <p>
     * API đơn giản để lấy thông tin liên hệ của một khách hàng cụ thể:
     * - Tên khách hàng
     * - Link Facebook (có thể click mở tab mới + copy button)
     * - Số điện thoại (có thể copy + tel link)
     * 
     * @param id - ID của video cần lấy thông tin liên hệ khách hàng
     * @return ResponseEntity chứa thông tin liên hệ khách hàng
     * - 200 OK: Lấy thông tin thành công
     * - 404 NOT_FOUND: Video không tồn tại
     * - 400 BAD_REQUEST: ID không hợp lệ
     */
    @GetMapping("/{id}/customer-contact")
    public ResponseEntity<ApiResponse<CustomerContactDto>> getCustomerContactById(@PathVariable Long id) {
        String tenantId = TenantContext.getTenantId();
        log.info("[Tenant: {}] Received request to get customer contact for video ID: {}", tenantId, id);

        CustomerContactDto contact = videoService.getCustomerContactById(id);
        
        return ResponseUtil.ok("Lấy thông tin liên hệ khách hàng thành công", contact);
    }

    /**
     * Apply billImageUrl permission logic to a list of videos
     * <p>
     * Logic phân quyền cho billImageUrl:
     * 1. Super Admin (realm admin): Xem được tất cả billImageUrl
     * 2. Resource Admin (video-veo3-be): Chỉ xem được billImageUrl của video do mình tạo (so sánh createdBy)
     * 3. User khác: billImageUrl luôn là null
     *
     * @param videos Danh sách video cần áp dụng logic phân quyền
     */
    private void applyBillImageUrlPermissions(List<VideoResponseDto> videos) {
        String currentUserName = jwtTokenService.getCurrentUserNameFromJwt();
        boolean isRealmAdmin = jwtTokenService.isRealmAdmin();
        boolean isResourceAdmin = jwtTokenService.isVideoVeo3BeAdmin();
        
        for (VideoResponseDto video : videos) {
            // Logic phân quyền cho billImageUrl:
            // 1. Super Admin (realm admin): Xem được tất cả billImageUrl
            // 2. Resource Admin (video-veo3-be): Chỉ xem được billImageUrl của video do mình tạo
            // 3. User khác: billImageUrl luôn là null
            if (!isRealmAdmin) {
                if (!isResourceAdmin || !currentUserName.equals(video.getCreatedBy())) {
                    video.setBillImageUrl(null);
                }
            }
        }
    }
}