package com.github.zeng.alt.security.rbac.client.config;

import com.github.zeng.alt.message.MessageQueueTemplate;
import com.github.zeng.alt.security.rbac.client.RouteTemplateRegistrar;
import com.github.zeng.alt.security.rbac.client.collector.ReactiveRouteTemplateCollector;
import com.github.zeng.alt.security.rbac.client.collector.RouteTemplateCollector;
import com.github.zeng.alt.security.rbac.client.collector.ServletRouteTemplateCollector;
import com.github.zeng.alt.security.rbac.client.properties.RbacClientProperties;
import com.github.zeng.alt.security.rbac.client.registrar.DirectRouteTemplateRegistrar;
import com.github.zeng.alt.security.rbac.client.registrar.MessageRouteTemplateRegistrar;
import com.github.zeng.alt.security.rbac.client.RbacClientRuntimeHints;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@AutoConfiguration
@EnableConfigurationProperties(RbacClientProperties.class)
@ImportRuntimeHints(RbacClientRuntimeHints.class)
public class RbacClientAutoConfiguration {

    @Configuration
    @ConditionalOnClass(org.springframework.boot.actuate.endpoint.annotation.Endpoint.class)
    public static class ActuatorEndpointConfiguration {

        @Bean
        @ConditionalOnBean(RouteTemplateRegistrar.class)
        public com.github.zeng.alt.security.rbac.client.actuator.RbacClientActuatorEndpoint rbacClientActuatorEndpoint(
                RouteTemplateRegistrar routeTemplateRegistrar) {
            return new com.github.zeng.alt.security.rbac.client.actuator.RbacClientActuatorEndpoint(
                    routeTemplateRegistrar);
        }
    }

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = "org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping")
    public static class ServletConfiguration {

        @Bean
        @ConditionalOnMissingBean(RouteTemplateCollector.class)
        public ServletRouteTemplateCollector servletRouteTemplateCollector(
                java.util.List<org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping> handlerMappings) {
            return new ServletRouteTemplateCollector(handlerMappings);
        }

        @Configuration
        @ConditionalOnClass(RouteTemplateManager.class)
        public static class ServletDirect {

            @Bean
            @ConditionalOnMissingBean(RouteTemplateRegistrar.class)
            public DirectRouteTemplateRegistrar servletDirectRegistrar(
                    RouteTemplateCollector servletRouteTemplateCollector,
                    RbacClientProperties properties,
                    RouteTemplateManager routeTemplateManager) {
                return new DirectRouteTemplateRegistrar(
                        servletRouteTemplateCollector, properties, routeTemplateManager);
            }
        }

        @Configuration
        @ConditionalOnClass(MessageQueueTemplate.class)
        @ConditionalOnMissingClass("com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager")
        public static class ServletMessage {

            @Bean
            @ConditionalOnMissingBean(RouteTemplateRegistrar.class)
            public MessageRouteTemplateRegistrar servletMessageRegistrar(
                    RouteTemplateCollector servletRouteTemplateCollector,
                    RbacClientProperties properties,
                    MessageQueueTemplate messageQueueTemplate) {
                return new MessageRouteTemplateRegistrar(
                        servletRouteTemplateCollector, properties, messageQueueTemplate);
            }
        }
    }

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnClass(name = "org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping")
    public static class ReactiveConfiguration {

        @Bean
        @ConditionalOnMissingBean(RouteTemplateCollector.class)
        public ReactiveRouteTemplateCollector reactiveRouteTemplateCollector(
                java.util.List<org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping> handlerMappings) {
            return new ReactiveRouteTemplateCollector(handlerMappings);
        }

        @Configuration
        @ConditionalOnClass(RouteTemplateManager.class)
        public static class ReactiveDirect {

            @Bean
            @ConditionalOnMissingBean(RouteTemplateRegistrar.class)
            public DirectRouteTemplateRegistrar reactiveDirectRegistrar(
                    RouteTemplateCollector reactiveRouteTemplateCollector,
                    RbacClientProperties properties,
                    RouteTemplateManager routeTemplateManager) {
                return new DirectRouteTemplateRegistrar(
                        reactiveRouteTemplateCollector, properties, routeTemplateManager);
            }
        }

        @Configuration
        @ConditionalOnClass(MessageQueueTemplate.class)
        @ConditionalOnMissingClass("com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager")
        public static class ReactiveMessage {

            @Bean
            @ConditionalOnMissingBean(RouteTemplateRegistrar.class)
            public MessageRouteTemplateRegistrar reactiveMessageRegistrar(
                    RouteTemplateCollector reactiveRouteTemplateCollector,
                    RbacClientProperties properties,
                    MessageQueueTemplate messageQueueTemplate) {
                return new MessageRouteTemplateRegistrar(
                        reactiveRouteTemplateCollector, properties, messageQueueTemplate);
            }
        }
    }
}
