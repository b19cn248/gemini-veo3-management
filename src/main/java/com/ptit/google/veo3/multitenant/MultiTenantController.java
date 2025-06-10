package com.ptit.google.veo3.multitenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Controller để quản lý và monitor multi-tenant system
 * Cung cấp các endpoints để:
 * - Xem danh sách tenants đang hoạt động
 * - Đóng DataSource cho tenant cụ thể
 * - Xem thông tin tenant hiện tại
 *
 * @author Generated
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/multitenant")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MultiTenantController {

    private final TenantDataSourceConfig dataSourceConfig;

    /**
     * GET /api/v1/multitenant/active-tenants
     * Lấy danh sách các tenants đang có DataSource active
     *
     * @return ResponseEntity chứa danh sách active tenants
     */
    @GetMapping("/active-tenants")
    public ResponseEntity<Map<String, Object>> getActiveTenants() {
        log.info("Received request to get active tenants");

        try {
            Set<String> activeTenants = dataSourceConfig.getActiveTenants();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Lấy danh sách tenants thành công");
            response.put("data", Map.of(
                    "activeTenants", activeTenants,
                    "count", activeTenants.size()
            ));
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting active tenants: ", e);
            return createErrorResponse("Lỗi khi lấy danh sách tenants: " + e.getMessage());
        }
    }

    /**
     * GET /api/v1/multitenant/current-tenant
     * Lấy thông tin tenant hiện tại từ context
     *
     * @return ResponseEntity chứa thông tin tenant hiện tại
     */
    @GetMapping("/current-tenant")
    public ResponseEntity<Map<String, Object>> getCurrentTenant() {
        log.info("Received request to get current tenant");

        try {
            String currentTenant = TenantContext.getTenantId();
            boolean hasContext = TenantContext.isSet();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Lấy thông tin tenant hiện tại thành công");
            response.put("data", Map.of(
                    "currentTenant", currentTenant != null ? currentTenant : "null",
                    "hasContext", hasContext,
                    "isActive", currentTenant != null && dataSourceConfig.hasDataSource(currentTenant)
            ));
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting current tenant: ", e);
            return createErrorResponse("Lỗi khi lấy thông tin tenant hiện tại: " + e.getMessage());
        }
    }

    /**
     * POST /api/v1/multitenant/close-datasource/{tenantId}
     * Đóng DataSource cho tenant cụ thể
     * Sử dụng để cleanup resources hoặc reset connection pool
     *
     * @param tenantId - ID của tenant cần đóng DataSource
     * @return ResponseEntity xác nhận đã đóng DataSource
     */
    @PostMapping("/close-datasource/{tenantId}")
    public ResponseEntity<Map<String, Object>> closeDataSource(@PathVariable String tenantId) {
        log.info("Received request to close DataSource for tenant: {}", tenantId);

        try {
            if (!dataSourceConfig.hasDataSource(tenantId)) {
                return createErrorResponse("DataSource cho tenant '" + tenantId + "' không tồn tại");
            }

            dataSourceConfig.closeDataSource(tenantId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đóng DataSource cho tenant '" + tenantId + "' thành công");
            response.put("data", Map.of("closedTenant", tenantId));
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error closing DataSource for tenant '{}': ", tenantId, e);
            return createErrorResponse("Lỗi khi đóng DataSource: " + e.getMessage());
        }
    }

    /**
     * GET /api/v1/multitenant/health
     * Health check endpoint cho multi-tenant system
     *
     * @return ResponseEntity chứa thông tin health của system
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.debug("Multi-tenant health check requested");

        try {
            Set<String> activeTenants = dataSourceConfig.getActiveTenants();
            String currentTenant = TenantContext.getTenantId();

            Map<String, Object> healthInfo = new HashMap<>();
            healthInfo.put("status", "UP");
            healthInfo.put("activeTenants", activeTenants.size());
            healthInfo.put("currentTenant", currentTenant);
            healthInfo.put("hasContext", TenantContext.isSet());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Multi-tenant system đang hoạt động bình thường");
            response.put("data", healthInfo);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in multi-tenant health check: ", e);

            Map<String, Object> healthInfo = new HashMap<>();
            healthInfo.put("status", "DOWN");
            healthInfo.put("error", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Multi-tenant system gặp lỗi");
            response.put("data", healthInfo);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * Helper method để tạo error response
     *
     * @param message - Error message
     * @return ResponseEntity chứa error response
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("data", null);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.badRequest().body(response);
    }
}