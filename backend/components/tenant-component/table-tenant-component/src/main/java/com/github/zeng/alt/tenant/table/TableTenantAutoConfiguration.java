package com.github.zeng.alt.tenant.table;

import com.github.zeng.alt.tenant.core.TenantAutoConfiguration;
import com.github.zeng.alt.tenant.core.TenantProperties;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;

import java.util.Set;

/**
 * 表级隔离自动配置。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@AutoConfiguration(after = TenantAutoConfiguration.class)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnClass(EntityManagerFactory.class)
@ConditionalOnProperty(prefix = "alt.tenant", name = "enabled", havingValue = "true")
public class TableTenantAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantTableRegistry.class)
    public TenantTableRegistry tenantTableRegistry(
            ObjectProvider<EntityManagerFactory> entityManagerFactory) {
        return new TenantTableRegistry(entityManagerFactory, Set.of());
    }

    @Bean
    @ConditionalOnMissingBean(TableSuffixSqlRewriter.class)
    public TableSuffixSqlRewriter tableSuffixSqlRewriter(
            TenantTableRegistry tableRegistry, TenantProperties properties) {
        return new TableSuffixSqlRewriter(tableRegistry, properties);
    }
}
