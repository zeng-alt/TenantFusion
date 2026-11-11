package com.github.zeng.alt.tenant.remote;

import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import com.github.zeng.alt.tenant.api.TenantSingleDataSourceProvider;
import com.github.zeng.alt.tenant.core.TenantAutoConfiguration;
import com.github.zeng.alt.tenant.remote.client.TenantAdminApi;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;

/**
 * 远程租户源自动配置：经 OpenFeign 从 admin 读取租户元数据。
 * <p>
 * 开关是 {@code alt.tenant.remote.enabled=true}（默认 false，不影响现有单体部署）。
 * 同样声明 {@code before = TenantAutoConfiguration}，注册成功后 {@code core} 的
 * 单库回退实现自动退让；若 classpath 上同时有 local 模块，local 的
 * {@code @ConditionalOnMissingBean(TenantMetadataProvider)} 语义使 local 优先，
 * 本配置的 provider Bean 不会创建。
 * <p>
 * 端点保持鉴权：用户链路透传当前请求 JWT，无用户上下文时用
 * {@code alt.tenant.remote.service-token}（M2M），无需开白名单。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
@AutoConfiguration(before = TenantAutoConfiguration.class)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@EnableConfigurationProperties(RemoteTenantProperties.class)
@EnableFeignClients(basePackages = "com.github.zeng.alt.tenant.remote.client")
@ConditionalOnProperty(prefix = "alt.tenant.remote", name = "enabled", havingValue = "true")
@CommonsLog
public class RemoteTenantStoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantRemoteSource.class)
    public TenantAdminClient tenantAdminClient(TenantAdminApi api,
                                               RemoteTenantProperties properties) {
        log.info("远程租户源已启用，admin 基地址=" + properties.getBaseUrl()
                + "，路径=" + properties.getPathPrefix());
        return new TenantAdminClient(api);
    }

    @Bean
    @ConditionalOnMissingBean(TenantMetadataProvider.class)
    public RemoteTenantMetadataProvider remoteTenantMetadataProvider(
            TenantRemoteSource tenantRemoteSource, RemoteTenantProperties properties) {
        return new RemoteTenantMetadataProvider(
                tenantRemoteSource, properties.getCacheTtl().toMillis());
    }

    @Bean
    @ConditionalOnMissingBean(TenantSingleDataSourceProvider.class)
    public RemoteTenantSingleDataSourceProvider remoteTenantSingleDataSourceProvider(
            RemoteTenantMetadataProvider remoteTenantMetadataProvider) {
        return new RemoteTenantSingleDataSourceProvider(remoteTenantMetadataProvider);
    }
}
