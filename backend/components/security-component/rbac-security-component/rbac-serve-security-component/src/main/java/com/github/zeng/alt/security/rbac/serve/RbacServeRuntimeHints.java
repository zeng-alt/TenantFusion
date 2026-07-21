package com.github.zeng.alt.security.rbac.serve;

import com.github.zeng.alt.security.rbac.serve.config.RbacAutoConfiguration;
import com.github.zeng.alt.security.rbac.serve.config.RbacReactiveAutoConfiguration;
import com.github.zeng.alt.security.rbac.serve.config.RbacWebAutoConfiguration;
import com.github.zeng.alt.security.rbac.serve.handler.AbstractReactiveResourceHandler;
import com.github.zeng.alt.security.rbac.serve.handler.AbstractResourceHandler;
import com.github.zeng.alt.security.rbac.serve.handler.HttpResourceHandler;
import com.github.zeng.alt.security.rbac.serve.handler.ReactiveHttpResourceHandler;
import com.github.zeng.alt.security.rbac.serve.handler.ReactiveResourceHandler;
import com.github.zeng.alt.security.rbac.serve.handler.ResourceHandler;
import com.github.zeng.alt.security.rbac.serve.locator.AbstractReactiveResourceLocator;
import com.github.zeng.alt.security.rbac.serve.locator.AbstractResourceLocator;
import com.github.zeng.alt.security.rbac.serve.locator.HttpResourceLocator;
import com.github.zeng.alt.security.rbac.serve.locator.ReactiveHttpResourceLocator;
import com.github.zeng.alt.security.rbac.serve.locator.ReactivePermissionLocator;
import com.github.zeng.alt.security.rbac.serve.locator.ReactiveResourceLocator;
import com.github.zeng.alt.security.rbac.serve.locator.ResourceLocator;
import com.github.zeng.alt.security.rbac.serve.manager.ParseManager;
import com.github.zeng.alt.security.rbac.serve.manager.RbacAccessAuthorizationManager;
import com.github.zeng.alt.security.rbac.serve.manager.ReactiveAdminAuthorizationManager;
import com.github.zeng.alt.security.rbac.serve.manager.ReactiveParseManager;
import com.github.zeng.alt.security.rbac.serve.manager.ReactiveRbacAccessAuthorizationManager;
import com.github.zeng.alt.security.rbac.serve.manager.ReactiveResourceQueryManager;
import com.github.zeng.alt.security.rbac.serve.manager.ResourceQueryManager;
import com.github.zeng.alt.security.rbac.serve.repository.DefaultRbacResourceService;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateEvent;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateTrie;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class RbacServeRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        registerType(hints,
                RbacAutoConfiguration.class,
                RbacWebAutoConfiguration.class,
                RbacReactiveAutoConfiguration.class,
                RouteTemplateManager.class,
                RouteTemplateTrie.class,
                RouteTemplateEvent.class,
                RbacResourceService.class,
                DefaultRbacResourceService.class,
                ResourceHandler.class,
                AbstractResourceHandler.class,
                HttpResourceHandler.class,
                ReactiveResourceHandler.class,
                AbstractReactiveResourceHandler.class,
                ReactiveHttpResourceHandler.class,
                ResourceLocator.class,
                AbstractResourceLocator.class,
                HttpResourceLocator.class,
                ReactiveResourceLocator.class,
                AbstractReactiveResourceLocator.class,
                ReactiveHttpResourceLocator.class,
                ReactivePermissionLocator.class,
                ParseManager.class,
                ResourceQueryManager.class,
                RbacAccessAuthorizationManager.class,
                ReactiveParseManager.class,
                ReactiveResourceQueryManager.class,
                ReactiveRbacAccessAuthorizationManager.class,
                ReactiveAdminAuthorizationManager.class
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
