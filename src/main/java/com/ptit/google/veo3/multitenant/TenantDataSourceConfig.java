package com.ptit.google.veo3.multitenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Configuration class để quản lý dynamic DataSource cho multi-tenant
 * FIXED: Auto-commit configuration issues
 *
 * @author Generated
 * @since 1.0
 */
@Component
@Slf4j
public class TenantDataSourceConfig {

    // Cache DataSource theo tenant để tối ưu performance
    private final ConcurrentMap<String, DataSource> dataSources = new ConcurrentHashMap<>();

    @Value("${spring.datasource.username:root}")
    private String username;

    @Value("${spring.datasource.password:root}")
    private String password;

    @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String driverClassName;

    @Value("${multitenant.datasource.url-pattern:jdbc:mysql://localhost:3306/%s?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true}")
    private String urlPattern;

    // HikariCP Configuration
    @Value("${multitenant.datasource.maximum-pool-size:10}")
    private int maximumPoolSize;

    @Value("${multitenant.datasource.minimum-idle:2}")
    private int minimumIdle;

    @Value("${multitenant.datasource.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${multitenant.datasource.idle-timeout:600000}")
    private long idleTimeout;

    @Value("${multitenant.datasource.max-lifetime:1800000}")
    private long maxLifetime;

    /**
     * Lấy DataSource cho tenant cụ thể
     * Nếu chưa có thì tạo mới và cache lại
     *
     * @param tenantId - Tenant identifier (database name)
     * @return DataSource cho tenant
     */
    public DataSource getDataSource(String tenantId) {
        return dataSources.computeIfAbsent(tenantId, this::createDataSource);
    }

    /**
     * Tạo DataSource mới cho tenant
     * FIXED: Auto-commit và transaction management issues
     *
     * @param tenantId - Tenant identifier (database name)
     * @return DataSource mới được tạo
     */
    private DataSource createDataSource(String tenantId) {
        log.info("Creating new DataSource for tenant: {}", tenantId);

        try {
            HikariConfig config = new HikariConfig();

            // Database connection settings
            config.setJdbcUrl(String.format(urlPattern, tenantId));
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName(driverClassName);

            // Connection pool settings
            config.setMaximumPoolSize(maximumPoolSize);
            config.setMinimumIdle(minimumIdle);
            config.setConnectionTimeout(connectionTimeout);
            config.setIdleTimeout(idleTimeout);
            config.setMaxLifetime(maxLifetime);

            // Pool name cho monitoring
            config.setPoolName("HikariPool-" + tenantId);

            // Performance tuning
            config.setLeakDetectionThreshold(60000); // 60 seconds
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(3000);

            // CRITICAL FIX: Auto-commit configuration
            // Explicitly set auto-commit to false for Spring Transaction Management
            config.setAutoCommit(false);

            // MySQL specific performance settings
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");

            // Transaction isolation level (READ_COMMITTED is recommended for multi-tenant)
            config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");

            // REMOVED problematic property that was causing the issue:
            // config.addDataSourceProperty("provider_disables_autocommit", "true");

            // Connection initialization
            config.setConnectionInitSql("SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO'");

            HikariDataSource dataSource = new HikariDataSource(config);

            log.info("DataSource created successfully for tenant: {} with auto-commit disabled", tenantId);

            return dataSource;

        } catch (Exception e) {
            log.error("Failed to create DataSource for tenant: {}", tenantId, e);
            throw new RuntimeException("Cannot create DataSource for tenant: " + tenantId, e);
        }
    }

    /**
     * Đóng DataSource cho tenant cụ thể
     * Sử dụng khi cần cleanup resources
     *
     * @param tenantId - Tenant identifier
     */
    public void closeDataSource(String tenantId) {
        DataSource dataSource = dataSources.remove(tenantId);
        if (dataSource instanceof HikariDataSource) {
            log.info("Closing DataSource for tenant: {}", tenantId);
            ((HikariDataSource) dataSource).close();
        }
    }

    /**
     * Đóng tất cả DataSources
     * Sử dụng khi shutdown application
     */
    public void closeAllDataSources() {
        log.info("Closing all DataSources...");
        dataSources.forEach((tenantId, dataSource) -> {
            if (dataSource instanceof HikariDataSource) {
                log.info("Closing DataSource for tenant: {}", tenantId);
                ((HikariDataSource) dataSource).close();
            }
        });
        dataSources.clear();
        log.info("All DataSources closed successfully");
    }

    /**
     * Lấy thông tin về các DataSources đang hoạt động
     * Sử dụng cho monitoring và debugging
     *
     * @return Set các tenant IDs đang có DataSource
     */
    public java.util.Set<String> getActiveTenants() {
        return dataSources.keySet();
    }

    /**
     * Kiểm tra xem DataSource cho tenant đã tồn tại chưa
     *
     * @param tenantId - Tenant identifier
     * @return true nếu DataSource đã tồn tại
     */
    public boolean hasDataSource(String tenantId) {
        return dataSources.containsKey(tenantId);
    }
}