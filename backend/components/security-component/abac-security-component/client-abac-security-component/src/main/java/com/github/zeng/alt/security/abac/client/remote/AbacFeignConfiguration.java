package com.github.zeng.alt.security.abac.client.remote;

import com.github.zeng.alt.security.abac.client.config.AbacClientProperties;
import feign.Request;
import feign.RequestInterceptor;
import org.springframework.cloud.openfeign.clientconfig.FeignClientConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;

/**
 * 仅作用于 abacPolicies 命名子上下文，不继承其他 Feign 客户端的用户凭据拦截器。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class AbacFeignConfiguration {
    @Bean
    public RequestInterceptor abacServiceRequestInterceptor(AbacClientProperties properties) {
        Assert.hasText(properties.getClientId(), "security.abac.client.client-id is required for remote source");
        Assert.hasText(properties.getServiceKey(), "security.abac.client.service-key is required for remote source");
        return new AbacServiceRequestInterceptor(properties);
    }

    @Bean
    public Request.Options abacRequestOptions(AbacClientProperties properties) {
        return new Request.Options(properties.getConnectTimeout().toMillis(), TimeUnit.MILLISECONDS,
                properties.getReadTimeout().toMillis(), TimeUnit.MILLISECONDS, false);
    }

    @Bean
    public FeignClientConfigurer abacFeignClientConfigurer() {
        return new FeignClientConfigurer() {
            @Override
            public boolean inheritParentConfiguration() {
                return false;
            }
        };
    }
}
