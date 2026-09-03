package com.github.zeng.alt.tenant.schema;

import com.github.zeng.alt.tenant.api.TenantDialect;
import com.github.zeng.alt.tenant.core.TenantAutoConfiguration;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;

/**
 * 模式级隔离自动配置。
 * <p>
 * 需要 classpath 上存在一个 {@link TenantDialect}——引入 {@code h2-tenant-component}
 * 或 {@code pg-tenant-component} 即可，它们按驱动自动生效。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@AutoConfiguration(after = TenantAutoConfiguration.class)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnBean(TenantDialect.class)
@ConditionalOnProperty(prefix = "alt.tenant", name = "enabled", havingValue = "true")
public class SchemaTenantAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SchemaConnectionCustomizer.class)
    public SchemaConnectionCustomizer schemaConnectionCustomizer(TenantDialect dialect) {
        return new SchemaConnectionCustomizer(dialect);
    }
}
