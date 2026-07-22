package com.github.zeng.alt.security.rbac.serve.manager;

import com.github.zeng.alt.security.rbac.serve.handler.ResourceHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.Assert;

import java.util.function.Supplier;

/**
 * Servlet 环境 RBAC 授权管理器。
 *
 * <p>实现 {@link AuthorizationManager}，通过 {@link ParseManager} 解析请求并分发到对应的
 * {@link ResourceHandler} 完成鉴权决策。</p>
 */
@Slf4j
public final class RbacAccessAuthorizationManager
		implements AuthorizationManager<RequestAuthorizationContext>, InitializingBean {

	private final ParseManager parseManager;

	public RbacAccessAuthorizationManager(ParseManager parseManager) {
		this.parseManager = parseManager;
	}

	@Override
	public AuthorizationDecision check(Supplier<Authentication> supplier, RequestAuthorizationContext object) {
		Authentication authentication = supplier.get();
		if (authentication == null) {
			log.warn("Authentication is null, denying access");
			return new AuthorizationDecision(false);
		}
		ResourceHandler handler = parseManager.parse(object.getRequest());
		boolean granted = handler.handler(authentication, object);
		log.debug("Authorization result for {} {}: {}",
				object.getRequest().getMethod(),
				object.getRequest().getRequestURI(),
				granted ? "GRANTED" : "DENIED");
		return new AuthorizationDecision(granted);
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(parseManager, "parseManager must not be null");
	}

}
