package com.github.zeng.alt.security.rbac.serve.config;

import com.github.zeng.alt.security.api.AuthorizationManagerProvider;
import com.github.zeng.alt.security.rbac.serve.handler.HttpResourceHandler;
import com.github.zeng.alt.security.rbac.serve.handler.ResourceHandler;
import com.github.zeng.alt.security.rbac.serve.locator.HttpResourceLocator;
import com.github.zeng.alt.security.rbac.serve.locator.ResourceLocator;
import com.github.zeng.alt.security.rbac.serve.manager.ParseManager;
import com.github.zeng.alt.security.rbac.serve.manager.RbacAccessAuthorizationManager;
import com.github.zeng.alt.security.rbac.serve.manager.ResourceQueryManager;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年10月09日 20:44
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RbacWebAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(HttpResourceLocator.class)
	public HttpResourceLocator httpResourceLocator(RbacResourceService rbacResourceService) {
		return new HttpResourceLocator(rbacResourceService);
	}



	@Bean
	@ConditionalOnMissingBean(ResourceQueryManager.class)
	public ResourceQueryManager resourceQueryManager(ObjectProvider<ResourceLocator> resourceLocators) {
		return new ResourceQueryManager(resourceLocators.stream().toList());
	}


	@Bean
	public ParseManager parseManager(ObjectProvider<ResourceHandler> resourceHandlers, ResourceQueryManager resourceQueryManager) {
		List<ResourceHandler> list = new ArrayList<>(resourceHandlers.orderedStream().toList());
		return new ParseManager(list, new HttpResourceHandler(resourceQueryManager));
	}

//	@Bean
//	public RbacAccessAuthorizationManager rbacAuthorizationManager(ParseManager parseManager) {
//		return new RbacAccessAuthorizationManager(parseManager);
//	}

	@Bean
	public AuthorizationManagerProvider<RequestAuthorizationContext> rbacAuthorizationManager(ParseManager parseManager) {
		return () -> new RbacAccessAuthorizationManager(parseManager);
	}

//	@Bean
//	public SecurityBuilderCustomizer rbacCustomizer(
//			AuthorizationManager<RequestAuthorizationContext> rbacAuthorizationManager) {
//		return http -> http.authorizeHttpRequests(a -> a.anyRequest().access(rbacAuthorizationManager));
//	}

}
