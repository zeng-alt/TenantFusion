package com.github.zeng.alt.tenant.core;

import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.tenant.api.TenantRouting;
import com.github.zeng.alt.tenant.api.TenantRoutingRegistry;

/**
 * 让 Hibernate 内部那些拿不到 Spring 容器的钩子（{@code StatementInspector}）能读到当前路由。
 * <p>
 * 优先取 {@link TenantContextHolder} 里过滤器已解析好的结果；没有则用注册表现场解析一次。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public final class TenantContextTracker {

    private static volatile TenantRoutingRegistry registry;

    private TenantContextTracker() {}

    /**
     * 由自动配置在启动时注入，避免 Hibernate 钩子反查容器。
     *
     * @param routingRegistry 路由注册表
     */
    static void setRegistry(TenantRoutingRegistry routingRegistry) {
        registry = routingRegistry;
    }

    /**
     * 取当前线程的租户路由。
     *
     * @return 路由，无租户上下文且注册表未就绪时返回 null
     */
    public static TenantRouting currentRouting() {
        TenantRouting cached = TenantContextHolder.getRouting();
        if (cached != null) {
            return cached;
        }
        TenantRoutingRegistry current = registry;
        if (current == null) {
            return null;
        }
        TenantRouting resolved = current.resolve(TenantContextHolder.getTenantId());
        TenantContextHolder.setRouting(resolved);
        return resolved;
    }
}
