package com.github.zeng.alt.security.rbac.client.actuator;

import com.github.zeng.alt.security.rbac.client.RouteTemplateRegistrar;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

@Endpoint(id = "rbac")
public class RbacClientActuatorEndpoint {

    private final RouteTemplateRegistrar routeTemplateRegistrar;

    public RbacClientActuatorEndpoint(RouteTemplateRegistrar routeTemplateRegistrar) {
        this.routeTemplateRegistrar = routeTemplateRegistrar;
    }

    @WriteOperation
    public String reRegisterRoutes() {
        routeTemplateRegistrar.reRegister();
        return "Route templates re-registered successfully";
    }
}
