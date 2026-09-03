package com.github.zeng.alt.tenant.hybrid;

import com.github.zeng.alt.tenant.api.TenantConnectionCustomizer;
import com.github.zeng.alt.tenant.api.TenantConnectionStrategy;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import com.github.zeng.alt.tenant.api.TenantRoutingRegistry;
import com.github.zeng.alt.tenant.api.TenantSqlRewriter;
import com.github.zeng.alt.tenant.core.TenantAutoConfiguration;
import com.github.zeng.alt.tenant.core.TenantProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;

/**
 * 混合模式自动配置。
 * <p>
 * 声明 {@code before = TenantAutoConfiguration.class}，使本模块的路由注册表先注册，
 * core 里那个带 {@code @ConditionalOnMissingBean} 的单档位实现随之退让。
 * 同时标 {@code @Primary} 以防装配顺序变化。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@AutoConfiguration(before = TenantAutoConfiguration.class)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnProperty(prefix = "alt.tenant", name = "enabled", havingValue = "true")
public class HybridTenantAutoConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean(HybridTenantRoutingRegistry.class)
    public HybridTenantRoutingRegistry hybridTenantRoutingRegistry(
            TenantProperties properties,
            ObjectProvider<TenantMetadataProvider> metadataProvider) {
        return new HybridTenantRoutingRegistry(properties, metadataProvider);
    }

    @Bean
    @ConditionalOnMissingBean(HybridTenantValidator.class)
    public HybridTenantValidator hybridTenantValidator(
            TenantRoutingRegistry registry,
            ObjectProvider<TenantMetadataProvider> metadataProvider,
            ObjectProvider<TenantConnectionStrategy> strategies,
            ObjectProvider<TenantConnectionCustomizer> customizers,
            ObjectProvider<TenantSqlRewriter> rewriters) {
        return new HybridTenantValidator(
                registry, metadataProvider, strategies, customizers, rewriters);
    }
}
