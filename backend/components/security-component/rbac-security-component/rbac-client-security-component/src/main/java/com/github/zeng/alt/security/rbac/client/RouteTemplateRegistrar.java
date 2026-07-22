package com.github.zeng.alt.security.rbac.client;

import com.github.zeng.alt.security.rbac.client.collector.RouteTemplateCollector;
import com.github.zeng.alt.security.rbac.client.properties.RbacClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.Ordered;

import java.util.List;

/**
 * 路由模板注册器抽象基类。
 *
 * <p>实现 {@link SmartInitializingSingleton}，在应用启动完成后自动触发路由模板收集和注册。
 * 子类决定注册方式：直接注册（单体）或消息队列通知（微服务）。</p>
 */
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

    /**
     * 执行路由模板注册。
     * <p>可由 {@code POST /actuator/rbac} 触发重新注册。</p>
     */
    public void reRegister() {
        if (!properties.isEnabled()) {
            log.info("Rbac client route registration is disabled (rbac.client.enabled=false)");
            return;
        }
        List<String> templates = collector.collectTemplates();
        if (templates.isEmpty()) {
            log.warn("No route templates found to register");
            return;
        }
        String contextPath = properties.getContextPath();
        log.debug("Re-registering {} route templates for contextPath '{}'", templates.size(), contextPath);
        doRegister(contextPath, templates);
    }

    protected abstract void doRegister(String contextPath, List<String> templates);

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}
