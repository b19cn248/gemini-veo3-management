package com.ptit.google.veo3.multitenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor để extract tenant information từ HTTP header
 * và set vào TenantContext cho request hiện tại
 *
 * Chạy trước khi request đến Controller và sau khi response được trả về
 *
 * @author Generated
 * @since 1.0
 */
@Component
@Slf4j
public class TenantInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER_NAME = "db";
    private static final String DEFAULT_TENANT = "video_management"; // Database mặc định

    /**
     * Chạy trước khi request được xử lý bởi Controller
     * Extract tenant từ header và set vào TenantContext
     *
     * @param request - HTTP request
     * @param response - HTTP response
     * @param handler - Handler object
     * @return true để tiếp tục xử lý request, false để dừng
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.debug("Processing request: {} {}", request.getMethod(), request.getRequestURI());

        try {
            String tenantId = extractTenantFromRequest(request);

            // Validate tenant ID
            if (!isValidTenantId(tenantId)) {
                log.warn("Invalid tenant ID received: {}", tenantId);
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"success\": false, \"message\": \"Invalid tenant identifier\"}");
                return false;
            }

            // Set tenant context
            TenantContext.setTenantId(tenantId);
            log.info("Tenant context set for request: {} -> {}", request.getRequestURI(), tenantId);

            return true;

        } catch (Exception e) {
            log.error("Error processing tenant context in preHandle", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return false;
        }
    }

    /**
     * Chạy sau khi request được xử lý xong
     * Clear tenant context để tránh memory leak
     *
     * @param request - HTTP request
     * @param response - HTTP response
     * @param handler - Handler object
     * @param ex - Exception nếu có
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            String tenantId = TenantContext.getTenantId();
            TenantContext.clear();
            log.debug("Tenant context cleared for request: {} (was: {})",
                    request.getRequestURI(), tenantId);
        } catch (Exception e) {
            log.error("Error clearing tenant context in afterCompletion", e);
        }
    }

    /**
     * Extract tenant ID từ HTTP request
     * Thứ tự ưu tiên: Header -> Query Parameter -> Default
     *
     * @param request - HTTP request
     * @return Tenant ID
     */
    private String extractTenantFromRequest(HttpServletRequest request) {
        // 1. Kiểm tra header trước
        String tenantId = request.getHeader(TENANT_HEADER_NAME);

        // 2. Nếu không có header, kiểm tra query parameter
        if (!StringUtils.hasText(tenantId)) {
            tenantId = request.getParameter(TENANT_HEADER_NAME);
        }

        // 3. Nếu vẫn không có, sử dụng default
        if (!StringUtils.hasText(tenantId)) {
            tenantId = DEFAULT_TENANT;
            log.debug("No tenant specified, using default: {}", DEFAULT_TENANT);
        }

        return tenantId.trim().toLowerCase(); // Normalize tenant ID
    }

    /**
     * Validate tenant ID để đảm bảo security
     * Chỉ cho phép alphanumeric và underscore để tránh SQL injection
     *
     * @param tenantId - Tenant ID cần validate
     * @return true nếu valid
     */
    private boolean isValidTenantId(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }

        // Kiểm tra length (1-50 ký tự)
        if (tenantId.length() < 1 || tenantId.length() > 50) {
            return false;
        }

        // Chỉ cho phép alphanumeric và underscore
        return tenantId.matches("^[a-zA-Z0-9_]+$");
    }
}