package com.github.zeng.alt.tenant.remote.client;

import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.tenant.remote.RemoteTenantProperties;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 服务间鉴权透传拦截器（Feign {@code RequestInterceptor} 惯用法）。
 * <p>
 * 优先级：当前请求的 {@code Authorization} 原样透传（用户链路，admin 侧走正常 JWT 校验）；
 * 无请求上下文或无头时，改发 {@code X-Tenant-Service-Key} 静态密钥
 * （定时任务、启动预热等 M2M 场景，admin 侧仅放行元数据端点）。
 * 调用方已显式置头时不覆盖。租户头同理：请求头优先，其次 {@link TenantContextHolder}。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
public class TenantAuthRequestInterceptor implements RequestInterceptor {

    static final String AUTHORIZATION = "Authorization";

    /** 服务密钥头，必须与 local 侧过滤器同名 */
    static final String SERVICE_KEY_HEADER = "X-Tenant-Service-Key";

    private final RemoteTenantProperties properties;

    public TenantAuthRequestInterceptor(RemoteTenantProperties properties) {
        this.properties = properties;
    }

    @Override
    public void apply(RequestTemplate template) {
        if (!template.headers().containsKey(AUTHORIZATION)) {
            String auth = currentRequestHeader(AUTHORIZATION);
            if (StringUtils.hasText(auth)) {
                template.header(AUTHORIZATION, auth.trim());
            } else if (StringUtils.hasText(properties.getServiceToken())
                    && !template.headers().containsKey(SERVICE_KEY_HEADER)) {
                template.header(SERVICE_KEY_HEADER, properties.getServiceToken().trim());
            }
        }
        String tenantHeader = properties.getTenantHeader();
        if (StringUtils.hasText(tenantHeader) && !template.headers().containsKey(tenantHeader)) {
            String tenant = currentRequestHeader(tenantHeader);
            if (!StringUtils.hasText(tenant)) {
                tenant = TenantContextHolder.getTenantId();
            }
            if (StringUtils.hasText(tenant)) {
                template.header(tenantHeader, tenant.trim());
            }
        }
    }

    private String currentRequestHeader(String name) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            String value = servlet.getRequest().getHeader(name);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
