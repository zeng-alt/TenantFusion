package com.github.zeng.alt.security.rbac.serve.router;

import com.github.zeng.alt.event.annotation.EventSubscription;

import lombok.RequiredArgsConstructor;

/**
 * 接收远程路由模板事件；本地路由管理不依赖 Broker。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class RouteTemplateEventListener {
    private final RouteTemplateManager manager;

    @EventSubscription(id = "rbac-route-template", binding = "route-template")
    public void onRouteTemplate(RouteTemplateEvent event) {
        manager.addRouteTemplate(event.getContextPath(), event.getTemplates());
    }
}
