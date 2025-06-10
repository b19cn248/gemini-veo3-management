package com.ptit.google.veo3.multitenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Custom DataSource implementation để tự động routing connection
 * dựa trên tenant context hiện tại
 *
 * Kế thừa AbstractDataSource để implement chỉ các method cần thiết
 * và delegate các operations khác cho underlying DataSource
 *
 * @author Generated
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class TenantAwareDataSource extends AbstractDataSource {

    private final TenantDataSourceConfig dataSourceConfig;
    private final String defaultTenantId;

    /**
     * Core method để lấy Connection cho tenant hiện tại
     * Được gọi bởi JPA/Hibernate khi cần database connection
     *
     * @return Connection cho tenant hiện tại
     * @throws SQLException nếu không thể tạo connection
     */
    @Override
    public Connection getConnection() throws SQLException {
        String tenantId = getCurrentTenantId();
        log.debug("Getting connection for tenant: {}", tenantId);

        try {
            DataSource targetDataSource = dataSourceConfig.getDataSource(tenantId);
            Connection connection = targetDataSource.getConnection();

            log.debug("Connection obtained successfully for tenant: {}", tenantId);
            return connection;

        } catch (SQLException e) {
            log.error("Failed to get connection for tenant: {}", tenantId, e);
            throw new SQLException("Cannot obtain connection for tenant: " + tenantId, e);
        }
    }

    /**
     * Overloaded method với username và password
     * Delegate đến getConnection() vì authentication được handle ở DataSource level
     *
     * @param username - Database username (ignored)
     * @param password - Database password (ignored)
     * @return Connection cho tenant hiện tại
     * @throws SQLException nếu không thể tạo connection
     */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        // Username và password được config ở DataSource level, không sử dụng ở đây
        log.debug("getConnection called with username: {} (delegating to getConnection())", username);
        return getConnection();
    }

    /**
     * Xác định tenant ID hiện tại từ TenantContext
     * Fallback về default tenant nếu không có context
     *
     * @return Tenant ID hiện tại
     */
    private String getCurrentTenantId() {
        String tenantId = TenantContext.getTenantId();

        if (!StringUtils.hasText(tenantId)) {
            log.debug("No tenant context found, using default tenant: {}", defaultTenantId);
            return defaultTenantId;
        }

        return tenantId;
    }

    /**
     * Check xem DataSource có bị wrap không
     * Required cho Spring framework
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return (iface.isInstance(this) ||
                getCurrentDataSource().isWrapperFor(iface));
    }

    /**
     * Unwrap DataSource nếu cần
     * Required cho Spring framework
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return (T) this;
        }
        return getCurrentDataSource().unwrap(iface);
    }

    /**
     * Lấy DataSource hiện tại dựa trên tenant context
     *
     * @return DataSource cho tenant hiện tại
     */
    private DataSource getCurrentDataSource() {
        String tenantId = getCurrentTenantId();
        return dataSourceConfig.getDataSource(tenantId);
    }

    /**
     * Override toString() để debugging
     */
    @Override
    public String toString() {
        String tenantId = getCurrentTenantId();
        return String.format("TenantAwareDataSource{currentTenant='%s', defaultTenant='%s'}",
                tenantId, defaultTenantId);
    }
}