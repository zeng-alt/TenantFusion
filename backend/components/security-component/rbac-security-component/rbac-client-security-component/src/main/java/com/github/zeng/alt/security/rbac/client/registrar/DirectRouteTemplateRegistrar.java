package com.github.zeng.alt.security.rbac.client.registrar;

import com.github.zeng.alt.security.rbac.client.RouteTemplateRegistrar;
import com.github.zeng.alt.security.rbac.client.collector.RouteTemplateCollector;
import com.github.zeng.alt.security.rbac.client.properties.RbacClientProperties;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;

import java.util.List;

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
        routeTemplateManager.addRouteTemplate(contextPath, templates);
    }
}
