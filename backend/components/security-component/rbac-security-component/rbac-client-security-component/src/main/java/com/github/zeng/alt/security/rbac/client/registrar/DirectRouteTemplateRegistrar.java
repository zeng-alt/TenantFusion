package com.github.zeng.alt.security.rbac.client.registrar;

import com.github.zeng.alt.security.rbac.client.RouteTemplateRegistrar;
import com.github.zeng.alt.security.rbac.client.collector.RouteTemplateCollector;
import com.github.zeng.alt.security.rbac.client.properties.RbacClientProperties;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 单体模式路由注册器。
 *
 * <p>直接在同一 JVM 内调用 {@link RouteTemplateManager#addRouteTemplate(String, List)} 注册路由模板，
 * 无需经过消息队列。适用于 Client 和 Serve 在同一个进程中的部署场景。</p>
 */
@Slf4j
public class DirectRouteTemplateRegistrar extends RouteTemplateRegistrar {

    private final RouteTemplateManager routeTemplateManager;

    public DirectRouteTemplateRegistrar(RouteTemplateCollector collector,
                                        RbacClientProperties properties,
                                        RouteTemplateManager routeTemplateManager) {
        super(collector, properties);
        this.routeTemplateManager = routeTemplateManager;
    }

    @Override
    protected void doRegister(String contextPath, List<String> templates) {
        log.info("Directly registering {} templates with RouteTemplateManager", templates.size());
        routeTemplateManager.addRouteTemplate(contextPath, templates);
    }
}
