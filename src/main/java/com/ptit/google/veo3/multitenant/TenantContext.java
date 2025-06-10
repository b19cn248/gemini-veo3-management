package com.ptit.google.veo3.multitenant;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-safe context để lưu trữ thông tin tenant hiện tại trong request
 * Sử dụng ThreadLocal để đảm bảo mỗi thread có tenant context riêng biệt
 *
 * @author Generated
 * @since 1.0
 */
@Slf4j
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    /**
     * Set tenant identifier cho thread hiện tại
     *
     * @param tenantId - Tenant identifier (database name)
     */
    public static void setTenantId(String tenantId) {
        log.debug("Setting tenant context: {}", tenantId);
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Lấy tenant identifier của thread hiện tại
     *
     * @return Tenant identifier hoặc null nếu chưa được set
     */
    public static String getTenantId() {
        String tenantId = CURRENT_TENANT.get();
        log.debug("Getting tenant context: {}", tenantId);
        return tenantId;
    }

    /**
     * Clear tenant context cho thread hiện tại
     * Quan trọng: Phải gọi method này sau khi xử lý xong request
     * để tránh memory leak trong thread pool
     */
    public static void clear() {
        String tenantId = CURRENT_TENANT.get();
        log.debug("Clearing tenant context: {}", tenantId);
        CURRENT_TENANT.remove();
    }

    /**
     * Kiểm tra xem tenant context đã được set chưa
     *
     * @return true nếu tenant context đã được set
     */
    public static boolean isSet() {
        return CURRENT_TENANT.get() != null;
    }
}