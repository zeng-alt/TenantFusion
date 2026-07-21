package com.github.zeng.alt.security.rbac.client;

import com.github.zeng.alt.security.rbac.client.collector.RouteTemplateCollector;
import com.github.zeng.alt.security.rbac.client.properties.RbacClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.Ordered;

import java.util.List;

@Slf4j
public abstract class RouteTemplateRegistrar implements SmartInitializingSingleton, Ordered {

    private final RouteTemplateCollector collector;
    private final RbacClientProperties properties;

    protected RouteTemplateRegistrar(RouteTemplateCollector collector, RbacClientProperties properties) {
        this.collector = collector;
        this.properties = properties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        reRegister();
    }

    public void reRegister() {
        if (!properties.isEnabled()) {
            log.info("Rbac client route registration is disabled");
            return;
        }
        List<String> templates = collector.collectTemplates();
        if (templates.isEmpty()) {
            log.warn("No route templates found to register");
            return;
        }
        String contextPath = properties.getContextPath();
        doRegister(contextPath, templates);
        log.info("Registered {} route templates for contextPath '{}'", templates.size(), contextPath);
    }

    protected abstract void doRegister(String contextPath, List<String> templates);

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}
