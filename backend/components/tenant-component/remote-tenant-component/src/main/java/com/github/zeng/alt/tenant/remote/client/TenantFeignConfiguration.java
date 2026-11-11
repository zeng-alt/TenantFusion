package com.github.zeng.alt.tenant.remote.client;

import com.github.zeng.alt.tenant.remote.RemoteTenantProperties;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

/**
 * 租户 Feign 客户端专属配置。
 * <p>
 * 刻意不用 {@code @Configuration} 注解、不进组件扫描，仅由
 * {@link TenantAdminApi} 的 {@code configuration} 属性引用，
 * 只作用于本客户端，不污染同应用内其它 Feign 客户端
 * （如后续 remote-engine 的 Camunda 客户端）。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
public class TenantFeignConfiguration {

    @Bean
    public RequestInterceptor tenantAuthRequestInterceptor(RemoteTenantProperties properties) {
        return new TenantAuthRequestInterceptor(properties);
    }
}
