package com.github.zeng.alt.security.abac.client.remote;

import com.github.zeng.alt.security.abac.client.config.AbacClientProperties;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQueryService;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;

/**
 * 独立服务凭据，不读取请求线程的用户身份或 Authorization。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class AbacServiceRequestInterceptor implements RequestInterceptor {
    private final AbacClientProperties properties;

    @Override
    public void apply(RequestTemplate template) {
        template.removeHeader("Authorization");
        template.header(AbacPolicyQueryService.CLIENT_HEADER, properties.getClientId());
        template.header(AbacPolicyQueryService.KEY_HEADER, properties.getServiceKey());
    }
}
