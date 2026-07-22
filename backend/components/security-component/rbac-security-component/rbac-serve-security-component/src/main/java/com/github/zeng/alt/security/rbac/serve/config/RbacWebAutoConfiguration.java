package com.github.zeng.alt.security.rbac.serve.config;

import com.github.zeng.alt.security.api.AuthorizationManagerProvider;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.security.rbac.serve.handler.HttpResourceHandler;
import com.github.zeng.alt.security.rbac.serve.handler.ResourceHandler;
import com.github.zeng.alt.security.rbac.serve.locator.HttpResourceLocator;
import com.github.zeng.alt.security.rbac.serve.locator.PermissionLocator;
import com.github.zeng.alt.security.rbac.serve.locator.ResourceLocator;
import com.github.zeng.alt.security.rbac.serve.manager.AdminAuthorizationManager;
import com.github.zeng.alt.security.rbac.serve.manager.ParseManager;
import com.github.zeng.alt.security.rbac.serve.manager.RbacAccessAuthorizationManager;
import com.github.zeng.alt.security.rbac.serve.manager.ResourceQueryManager;
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
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Servlet 环境的 RBAC Web 自动配置。
 *
 * <p>在 {@code @ConditionalOnWebApplication(SERVLET)} 条件下生效，
 * 创建 Servlet 安全所需的 Bean：资源定位器、权限定位器、查询管理器、解析管理器和授权管理器。</p>
 */
@AutoConfiguration
@ConditionalOnBooleanProperty("security.context.enabled-access")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Slf4j
public class RbacWebAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(HttpResourceLocator.class)
	public HttpResourceLocator httpResourceLocator(RbacResourceService rbacResourceService) {
		log.debug("Creating HttpResourceLocator");
		return new HttpResourceLocator(rbacResourceService);
	}

	@Bean
	public PermissionLocator permissionLocator(RbacResourceService rbacResourceService) {
		log.debug("Creating PermissionLocator");
		return new PermissionLocator(rbacResourceService);
	}

	@Bean
	@ConditionalOnMissingBean(ResourceQueryManager.class)
	public ResourceQueryManager resourceQueryManager(ObjectProvider<ResourceLocator> resourceLocators) {
		List<ResourceLocator> locators = resourceLocators.orderedStream().toList();
		log.debug("Creating ResourceQueryManager with {} locators", locators.size());
		return new ResourceQueryManager(locators);
	}

	@Bean
	public ParseManager parseManager(ObjectProvider<ResourceHandler> resourceHandlers, RouteTemplateManager routeTemplateManager, ResourceQueryManager resourceQueryManager) {
		List<ResourceHandler> list = new ArrayList<>(resourceHandlers.orderedStream().toList());
		log.debug("Creating ParseManager with {} custom handlers + HttpResourceHandler fallback", list.size());
		return new ParseManager(list, new HttpResourceHandler(resourceQueryManager, routeTemplateManager));
	}

	@Bean
	@Order(5)
	public AuthorizationManagerProvider<RequestAuthorizationContext> adminAuthorizationManager(SecurityProperties securityProperties) {
		log.info("Registering AdminAuthorizationManagerProvider for Servlet environment (@Order 5)");
		return () -> new AdminAuthorizationManager(securityProperties);
	}

	@Bean
	@Order(10)
	public AuthorizationManagerProvider<RequestAuthorizationContext> rbacAuthorizationManager(ParseManager parseManager) {
		log.info("Registering RBAC AuthorizationManagerProvider for Servlet environment (@Order 10)");
		return () -> new RbacAccessAuthorizationManager(parseManager);
	}

}
