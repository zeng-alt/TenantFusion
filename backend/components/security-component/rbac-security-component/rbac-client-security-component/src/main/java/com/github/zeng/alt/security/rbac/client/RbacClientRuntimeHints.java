package com.github.zeng.alt.security.rbac.client;

import com.github.zeng.alt.security.rbac.client.actuator.RbacClientActuatorEndpoint;
import com.github.zeng.alt.security.rbac.client.collector.ReactiveRouteTemplateCollector;
import com.github.zeng.alt.security.rbac.client.collector.RouteTemplateCollector;
import com.github.zeng.alt.security.rbac.client.collector.ServletRouteTemplateCollector;
import com.github.zeng.alt.security.rbac.client.config.RbacClientAutoConfiguration;
import com.github.zeng.alt.security.rbac.client.properties.RbacClientProperties;
import com.github.zeng.alt.security.rbac.client.registrar.DirectRouteTemplateRegistrar;
import com.github.zeng.alt.security.rbac.client.registrar.MessageRouteTemplateRegistrar;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateEvent;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class RbacClientRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        registerType(hints,
                RbacClientAutoConfiguration.class,
                RbacClientAutoConfiguration.ActuatorEndpointConfiguration.class,
                RbacClientAutoConfiguration.ServletConfiguration.class,
                RbacClientAutoConfiguration.ServletConfiguration.ServletDirect.class,
                RbacClientAutoConfiguration.ServletConfiguration.ServletMessage.class,
                RbacClientAutoConfiguration.ReactiveConfiguration.class,
                RbacClientAutoConfiguration.ReactiveConfiguration.ReactiveDirect.class,
                RbacClientAutoConfiguration.ReactiveConfiguration.ReactiveMessage.class,
                RbacClientProperties.class,
                RouteTemplateRegistrar.class,
                DirectRouteTemplateRegistrar.class,
                MessageRouteTemplateRegistrar.class,
                RouteTemplateCollector.class,
                ServletRouteTemplateCollector.class,
                ReactiveRouteTemplateCollector.class,
                RbacClientActuatorEndpoint.class,
                RouteTemplateEvent.class
        );
    }

    private static void registerType(RuntimeHints hints, Class<?>... classes) {
        for (Class<?> clazz : classes) {
            hints.reflection().registerType(clazz,
                    MemberCategory.INTROSPECT_DECLARED_METHODS,
                    MemberCategory.DECLARED_FIELDS,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS);
        }
    }
}
