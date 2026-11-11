package com.github.zeng.alt.tenant.local;

import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import com.github.zeng.alt.tenant.api.TenantSingleDataSourceProvider;
import com.github.zeng.alt.tenant.core.TenantAutoConfiguration;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * 本地租户库自动配置：直连物理独立的租户库读取租户元数据。
 * <p>
 * 生效条件是配了 {@code alt.tenant.store.url}——未配置时（单库老部署）整个配置静默跳过，
 * {@code core} 里那个读主库的回退实现继续工作。声明 {@code before = TenantAutoConfiguration}，
 * 使本模块的 {@link TenantMetadataProvider} 先注册，{@code core} 的回退随之退让。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
@AutoConfiguration(before = TenantAutoConfiguration.class)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@EnableConfigurationProperties(TenantStoreProperties.class)
@ConditionalOnProperty(prefix = "alt.tenant.store", name = "url")
@CommonsLog
public class LocalTenantStoreAutoConfiguration {

    /** 租户库数据源 Bean 名，刻意不叫 dataSource，避免与主业务库冲突 */
    public static final String TENANT_STORE_DATA_SOURCE = "tenantStoreDataSource";

    /** 租户库 JdbcTemplate Bean 名 */
    public static final String TENANT_STORE_JDBC_TEMPLATE = "tenantStoreJdbcTemplate";

    /**
     * 租户库数据源（Bean 名刻意不叫 {@code dataSource}）。
     * <p>
     * 本模块引入第二数据源后，老模块里所有按类型注入的 {@code DataSource}
     * 会产生歧义，因此本配置同时把业务主库标为 {@code @Primary}
     * （见 {@link #markBusinessDataSourcePrimary}），老代码不用动；
     * 租户库一律用 Bean 名具名索取。
     */
    @Bean(TENANT_STORE_DATA_SOURCE)
    @ConditionalOnMissingBean(name = TENANT_STORE_DATA_SOURCE)
    public DataSource tenantStoreDataSource(TenantStoreProperties properties) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("tenant-store");
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        if (StringUtils.hasText(properties.getDriverClassName())) {
            config.setDriverClassName(properties.getDriverClassName());
        }
        config.setMaximumPoolSize(properties.getMaximumPoolSize());
        config.setMinimumIdle(properties.getMinimumIdle());
        log.info("创建租户库数据源，url=" + properties.getUrl());
        return new HikariDataSource(config);
    }

    @Bean(TENANT_STORE_JDBC_TEMPLATE)
    @ConditionalOnMissingBean(name = TENANT_STORE_JDBC_TEMPLATE)
    public JdbcTemplate tenantStoreJdbcTemplate(
            @org.springframework.beans.factory.annotation.Qualifier(TENANT_STORE_DATA_SOURCE)
            DataSource tenantStoreDataSource) {
        return new JdbcTemplate(tenantStoreDataSource);
    }

    @Bean
    @ConditionalOnMissingBean(TenantMetadataProvider.class)
    public LocalTenantMetadataProvider localTenantMetadataProvider(
            @org.springframework.beans.factory.annotation.Qualifier(TENANT_STORE_JDBC_TEMPLATE)
            JdbcTemplate tenantStoreJdbcTemplate,
            TenantStoreProperties properties) {
        return new LocalTenantMetadataProvider(tenantStoreJdbcTemplate, properties.getTable());
    }

    @Bean
    @ConditionalOnMissingBean(TenantSingleDataSourceProvider.class)
    public LocalTenantSingleDataSourceProvider localTenantSingleDataSourceProvider(
            LocalTenantMetadataProvider localTenantMetadataProvider) {
        return new LocalTenantSingleDataSourceProvider(localTenantMetadataProvider);
    }

    /**
     * 双数据源的定心丸：把业务主库（Boot 默认 Bean 名 {@code dataSource}）标为
     * {@code @Primary}，此后所有按类型注入的 {@code DataSource}/{@code JdbcTemplate}
     * 都归位到业务库，{@code lock} 等老模块零改动。
     * <p>
     * 租户库相关 Bean 一律用 Bean 名具名注入，不受此影响。
     * 若某应用希望租户库做主库，覆盖本 Bean 即可。
     *
     * @return BeanFactory 后处理器
     */
    @Bean
    @ConditionalOnMissingBean(name = "markBusinessDataSourcePrimary")
    public static BeanFactoryPostProcessor markBusinessDataSourcePrimary() {
        return beanFactory -> {
            if (beanFactory instanceof DefaultListableBeanFactory registry
                    && registry.containsBeanDefinition("dataSource")) {
                registry.getBeanDefinition("dataSource").setPrimary(true);
                log.info("业务主数据源 [dataSource] 已标为 @Primary");
            }
        };
    }
}
