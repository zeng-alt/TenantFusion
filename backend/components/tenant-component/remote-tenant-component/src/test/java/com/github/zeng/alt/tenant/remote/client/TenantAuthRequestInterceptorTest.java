package com.github.zeng.alt.tenant.remote.client;

import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.tenant.remote.RemoteTenantProperties;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 透传拦截器：用户 JWT 优先、serviceToken 兜底、已显式置头不覆盖。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
class TenantAuthRequestInterceptorTest {

    @AfterEach
    void cleanUp() {
        RequestContextHolder.resetRequestAttributes();
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("当前请求的 Authorization 与租户头原样透传")
    void forwardsCurrentRequestHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer user-jwt");
        request.addHeader("X-Tenant-Id", "t001");
        request.addHeader("X-Active-Tenant", "t002");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        TenantAuthRequestInterceptor interceptor = interceptor(null);
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).containsExactly("Bearer user-jwt");
        assertThat(template.headers().get("X-Tenant-Id")).containsExactly("t001");
        assertThat(template.headers().get("X-Active-Tenant")).containsExactly("t002");
    }

    @Test
    @DisplayName("无用户上下文时发服务密钥头，不伪造 Authorization")
    void fallsBackToServiceKey() {
        TenantContextHolder.setTenantId("t002");

        TenantAuthRequestInterceptor interceptor = interceptor("svc-secret");
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey("Authorization");
        assertThat(template.headers().get("X-Tenant-Service-Key")).containsExactly("svc-secret");
        assertThat(template.headers().get("X-Tenant-Id")).containsExactly("t002");
    }

    @Test
    @DisplayName("调用方已显式置头时不覆盖")
    void doesNotOverrideExplicitHeaders() {
        TenantAuthRequestInterceptor interceptor = interceptor("svc-secret");
        RequestTemplate template = new RequestTemplate();
        template.header("Authorization", "Bearer explicit");

        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).containsExactly("Bearer explicit");
    }

    private TenantAuthRequestInterceptor interceptor(String serviceToken) {
        RemoteTenantProperties properties = new RemoteTenantProperties();
        properties.setServiceToken(serviceToken);
        return new TenantAuthRequestInterceptor(properties);
    }
}
