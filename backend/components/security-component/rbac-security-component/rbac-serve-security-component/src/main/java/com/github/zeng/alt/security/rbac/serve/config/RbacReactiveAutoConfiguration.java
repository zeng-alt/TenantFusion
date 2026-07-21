package com.github.zeng.alt.security.rbac.serve.config;

import com.github.zeng.alt.security.api.ReactiveAuthorizationManagerProvider;
import com.github.zeng.alt.security.rbac.serve.handler.ReactiveHttpResourceHandler;
import com.github.zeng.alt.security.rbac.serve.handler.ReactiveResourceHandler;
import com.github.zeng.alt.security.rbac.serve.locator.ReactiveHttpResourceLocator;
import com.github.zeng.alt.security.rbac.serve.locator.ReactivePermissionLocator;
import com.github.zeng.alt.security.rbac.serve.locator.ReactiveResourceLocator;
import com.github.zeng.alt.security.rbac.serve.manager.ReactiveAdminAuthorizationManager;
import com.github.zeng.alt.security.rbac.serve.manager.ReactiveParseManager;
import com.github.zeng.alt.security.rbac.serve.manager.ReactiveRbacAccessAuthorizationManager;
import com.github.zeng.alt.security.rbac.serve.manager.ReactiveResourceQueryManager;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.server.authorization.AuthorizationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年11月26日 21:33
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class RbacReactiveAutoConfiguration {


    @Bean
    public ReactivePermissionLocator reactivePermissionLocator(RbacResourceService rbacResourceService) {
        return new ReactivePermissionLocator(rbacResourceService);
    }


    @Bean
    @ConditionalOnMissingBean(ReactiveHttpResourceLocator.class)
    public ReactiveHttpResourceLocator reactiveHttpResourceLocator(RbacResourceService rbacResourceService) {
        return new ReactiveHttpResourceLocator(rbacResourceService);
    }


    @Bean
    @ConditionalOnMissingBean(ReactiveResourceQueryManager.class)
    public ReactiveResourceQueryManager reactiveResourceQueryManager(ObjectProvider<ReactiveResourceLocator> reactiveResourceLocators) {
        return new ReactiveResourceQueryManager(reactiveResourceLocators.orderedStream().toList());
    }

    @Bean
    public ReactiveParseManager reactiveParseManager(ObjectProvider<ReactiveResourceHandler> reactiveResourceHandlers, ReactiveResourceQueryManager reactiveResourceQueryManager, ReactivePermissionLocator permissionLocator, com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager routeTemplateManager) {
        List<ReactiveResourceHandler> list = new ArrayList<>(reactiveResourceHandlers.orderedStream().toList());
        return new ReactiveParseManager(list, new ReactiveHttpResourceHandler(reactiveResourceQueryManager, routeTemplateManager, permissionLocator));
    }


//    @Bean
//    @Order(10)
//    public ReactiveRbacAccessAuthorizationManager rbacReactiveAuthorizationManager(ReactiveParseManager reactiveParseManager) {
//        return new ReactiveRbacAccessAuthorizationManager(reactiveParseManager);
//    }
//
//    @Bean
//    @Order(5)
//    public ReactiveAdminAuthorizationManager reactiveAdminAuthorizationManager(ReactiveParseManager reactiveParseManager) {
//        return new ReactiveAdminAuthorizationManager();
//    }

    @Bean
    @Order(10)
    public ReactiveAuthorizationManagerProvider<AuthorizationContext> rbacReactiveAuthorizationManager(ReactiveParseManager reactiveParseManager) {
        return () -> new ReactiveRbacAccessAuthorizationManager(reactiveParseManager);
    }

    @Bean
    @Order(5)
    public ReactiveAuthorizationManagerProvider<AuthorizationContext> reactiveAdminAuthorizationManager() {
        return ReactiveAdminAuthorizationManager::new;
    }


//    @Bean
//    public ServerHttpSecurityBuilderCustomizer rabcServerHttpSecurityBuilderCustomizer(ReactiveAuthorizationManager<AuthorizationContext> rbacReactiveAuthorizationManager) {
//        return http -> http.authorizeExchange(exchange -> exchange.anyExchange().access(rbacReactiveAuthorizationManager));
////        return http -> http.authorizeExchange(exchange -> exchange.anyExchange().access( AuthenticatedReactiveAuthorizationManager.authenticated()));
//    }
}
