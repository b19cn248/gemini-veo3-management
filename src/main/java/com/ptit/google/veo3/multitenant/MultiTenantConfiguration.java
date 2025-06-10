package com.ptit.google.veo3.multitenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.Properties;

/**
 * Main configuration class cho multi-tenant setup
 *
 * Cấu hình:
 * 1. DataSource với tenant awareness
 * 2. EntityManagerFactory với Hibernate properties
 * 3. TransactionManager
 * 4. Web interceptor để handle tenant context
 *
 * @author Generated
 * @since 1.0
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class MultiTenantConfiguration implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;
    private final TenantDataSourceConfig dataSourceConfig;

    @Value("${spring.jpa.hibernate.ddl-auto:update}")
    private String ddlAuto;

    @Value("${spring.jpa.show-sql:true}")
    private boolean showSql;

    @Value("${spring.jpa.properties.hibernate.dialect:org.hibernate.dialect.MySQLDialect}")
    private String hibernateDialect;

    @Value("${spring.jpa.properties.hibernate.format_sql:true}")
    private boolean formatSql;

    @Value("${spring.jpa.properties.hibernate.use_sql_comments:true}")
    private boolean useSqlComments;

    @Value("${multitenant.default-tenant:video_management}")
    private String defaultTenant;

    /**
     * Primary DataSource bean với tenant awareness
     * Thay thế DataSource mặc định của Spring Boot
     *
     * @return TenantAwareDataSource instance
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        log.info("Creating TenantAwareDataSource with default tenant: {}", defaultTenant);
        return new TenantAwareDataSource(dataSourceConfig, defaultTenant);
    }

    /**
     * EntityManagerFactory configuration cho JPA/Hibernate
     * Sử dụng TenantAwareDataSource làm data source
     *
     * @param dataSource - Tenant-aware DataSource
     * @return LocalContainerEntityManagerFactoryBean
     */
    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        log.info("Creating EntityManagerFactory for multi-tenant setup");

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.ptit.google.veo3.entity"); // Scan entity packages

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        // Hibernate properties
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", ddlAuto);
        properties.setProperty("hibernate.dialect", hibernateDialect);
        properties.setProperty("hibernate.show_sql", String.valueOf(showSql));
        properties.setProperty("hibernate.format_sql", String.valueOf(formatSql));
        properties.setProperty("hibernate.use_sql_comments", String.valueOf(useSqlComments));

        // Performance tuning
        properties.setProperty("hibernate.jdbc.batch_size", "20");
        properties.setProperty("hibernate.order_inserts", "true");
        properties.setProperty("hibernate.order_updates", "true");
        properties.setProperty("hibernate.jdbc.batch_versioned_data", "true");

        // Connection pool settings
        properties.setProperty("hibernate.connection.provider_disables_autocommit", "true");

        // Second level cache (optional)
        properties.setProperty("hibernate.cache.use_second_level_cache", "false");
        properties.setProperty("hibernate.cache.use_query_cache", "false");

        em.setJpaProperties(properties);

        log.info("EntityManagerFactory configured successfully");
        return em;
    }

    /**
     * Transaction Manager configuration
     * Sử dụng JpaTransactionManager cho JPA transactions
     *
     * @param entityManagerFactory - EntityManagerFactory instance
     * @return PlatformTransactionManager
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        log.info("Creating JpaTransactionManager for multi-tenant setup");

        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);

        log.info("JpaTransactionManager configured successfully");
        return transactionManager;
    }

    /**
     * Đăng ký TenantInterceptor để xử lý tenant context
     * Interceptor sẽ chạy cho tất cả requests đến API endpoints
     *
     * @param registry - InterceptorRegistry để đăng ký interceptor
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("Registering TenantInterceptor for path: /api/**");

        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/api/**") // Chỉ apply cho API endpoints
                .excludePathPatterns(
                        "/api/v1/health/**",      // Health check endpoints
                        "/api/v1/actuator/**",    // Actuator endpoints
                        "/swagger-ui/**",         // Swagger UI
                        "/v3/api-docs/**"         // OpenAPI docs
                );

        log.info("TenantInterceptor registered successfully");
    }

    /**
     * Shutdown hook để cleanup resources khi application stop
     * Đóng tất cả DataSource connections để tránh connection leaks
     */
    @Bean
    public MultiTenantShutdownHook shutdownHook() {
        return new MultiTenantShutdownHook(dataSourceConfig);
    }

    /**
     * Inner class để handle application shutdown
     */
    @RequiredArgsConstructor
    private static class MultiTenantShutdownHook {

        private final TenantDataSourceConfig dataSourceConfig;

        @jakarta.annotation.PreDestroy
        public void destroy() {
            log.info("Application shutting down, cleaning up multi-tenant resources...");
            dataSourceConfig.closeAllDataSources();
            log.info("Multi-tenant cleanup completed");
        }
    }
}