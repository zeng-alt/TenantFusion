package com.github.zeng.alt.tenant.local;

import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;

/**
 * 本地租户库 Web 自动配置：把 {@link TenantMetadataEndpoint} 挂到 Servlet 容器。
 * <p>
 * 仅 Web 应用且配了 {@code alt.tenant.store.url} 时生效——workflow 这类只引 remote
 * 的应用天然不会暴露该端点。端点只读元数据，不读不写业务库。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
@AutoConfiguration(after = LocalTenantStoreAutoConfiguration.class)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "alt.tenant.store", name = "url")
public class LocalTenantStoreWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantMetadataEndpoint.class)
    @ConditionalOnBean(TenantMetadataProvider.class)
    public TenantMetadataEndpoint tenantMetadataEndpoint(TenantMetadataProvider provider) {
        return new TenantMetadataEndpoint(provider);
    }
}
