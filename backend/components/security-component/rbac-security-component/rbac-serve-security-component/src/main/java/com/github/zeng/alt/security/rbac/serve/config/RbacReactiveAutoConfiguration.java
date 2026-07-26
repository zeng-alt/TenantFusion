package com.github.zeng.alt.security.rbac.serve.config;

import com.github.zeng.alt.security.api.ReactiveAuthorizationManagerProvider;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.security.rbac.serve.handler.ReactiveHttpResourceHandler;
import com.github.zeng.alt.security.rbac.serve.handler.ReactiveResourceHandler;
import com.github.zeng.alt.security.rbac.serve.locator.ReactiveHttpResourceSignageLocator;
import com.github.zeng.alt.security.rbac.serve.locator.ReactivePermissionLocator;
import com.github.zeng.alt.security.rbac.serve.manager.ReactiveAdminAuthorizationManager;
import com.github.zeng.alt.security.rbac.serve.manager.ReactiveParseManager;
import com.github.zeng.alt.security.rbac.serve.manager.ReactiveRbacAccessAuthorizationManager;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.server.authorization.AuthorizationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Reactive 环境的 RBAC WebFlux 自动配置。
 *
 * <p>在 {@code @ConditionalOnWebApplication(REACTIVE)} 条件下生效，
 * 创建 WebFlux 安全所需的 Bean：权限定位器、资源定位器、查询管理器、解析管理器和授权管理器。</p>
 */
@AutoConfiguration
@ConditionalOnBooleanProperty("security.context.enabled-access")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@Slf4j
public class RbacReactiveAutoConfiguration {

    @Bean
    public ReactivePermissionLocator reactivePermissionLocator(RbacResourceService rbacResourceService) {
        log.debug("Creating ReactivePermissionLocator");
        return new ReactivePermissionLocator(rbacResourceService);
    }

    @Bean
    public ReactiveParseManager reactiveParseManager(
            ObjectProvider<ReactiveResourceHandler> reactiveResourceHandlers,
            RouteTemplateManager routeTemplateManager,
            ReactivePermissionLocator permissionLocator,
            RbacResourceService rbacResourceService) {
        List<ReactiveResourceHandler> list = new ArrayList<>(reactiveResourceHandlers.orderedStream().toList());
        log.debug("Creating ReactiveParseManager with {} custom handlers + ReactiveHttpResourceHandler fallback", list.size());
        return new ReactiveParseManager(list, new ReactiveHttpResourceHandler(routeTemplateManager, permissionLocator, new ReactiveHttpResourceSignageLocator(rbacResourceService)));
    }

    @Bean
    @Order(10)
    public ReactiveAuthorizationManagerProvider<AuthorizationContext> rbacReactiveAuthorizationManager(ReactiveParseManager reactiveParseManager) {
        log.info("Registering ReactiveAuthorizationManagerProvider for WebFlux environment (@Order 10)");
        return () -> new ReactiveRbacAccessAuthorizationManager(reactiveParseManager);
    }

    @Bean
    @Order(5)
    public ReactiveAuthorizationManagerProvider<AuthorizationContext> reactiveAdminAuthorizationManager(SecurityProperties securityProperties) {
        log.info("Registering ReactiveAuthorizationManagerProvider for super admin bypass (@Order 5)");
        return () -> new ReactiveAdminAuthorizationManager(securityProperties);
    }

}
