package com.github.zeng.alt.tenant.database;

import com.github.zeng.alt.tenant.core.TenantAutoConfiguration;
import com.github.zeng.alt.tenant.core.TenantProperties;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;

/**
 * 库级隔离自动配置。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@AutoConfiguration(after = TenantAutoConfiguration.class)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnClass(name = "com.zaxxer.hikari.HikariDataSource")
@ConditionalOnProperty(prefix = "alt.tenant", name = "enabled", havingValue = "true")
public class DatabaseTenantAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantDataSourceRegistry.class)
    public TenantDataSourceRegistry tenantDataSourceRegistry(TenantProperties properties) {
        return new TenantDataSourceRegistry(properties);
    }

    @Bean
    @ConditionalOnMissingBean(TenantDataSourceConnectionStrategy.class)
    public TenantDataSourceConnectionStrategy tenantDataSourceConnectionStrategy(
            TenantDataSourceRegistry registry) {
        return new TenantDataSourceConnectionStrategy(registry);
    }
}
