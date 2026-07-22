package com.github.zeng.alt.security.rbac.client.actuator;

import com.github.zeng.alt.security.rbac.client.RouteTemplateRegistrar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

/**
 * Actuator 端点，用于手动触发路由模板重新注册。
 *
 * <p>端点路径：{@code POST /actuator/rbac}</p>
 * <p>调用 {@link RouteTemplateRegistrar#reRegister()} 重新收集并注册
 * 当前应用的所有 {@code @RequestMapping} 路径模板。</p>
 */
@Endpoint(id = "rbac")
@Slf4j
public class RbacClientActuatorEndpoint {

    private final RouteTemplateRegistrar routeTemplateRegistrar;

    public RbacClientActuatorEndpoint(RouteTemplateRegistrar routeTemplateRegistrar) {
        this.routeTemplateRegistrar = routeTemplateRegistrar;
    }

    @WriteOperation
    public String reRegisterRoutes() {
        log.info("Triggering route template re-registration via Actuator endpoint");
        routeTemplateRegistrar.reRegister();
        return "Route templates re-registered successfully";
    }
}
