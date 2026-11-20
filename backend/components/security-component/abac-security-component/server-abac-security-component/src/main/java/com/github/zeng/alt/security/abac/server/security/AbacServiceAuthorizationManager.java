package com.github.zeng.alt.security.abac.server.security;

import com.github.zeng.alt.security.abac.policy.AbacPolicyQueryService;
import com.github.zeng.alt.security.api.AuthorizationManagerProvider;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.function.Supplier;

/**
 * 服务身份只授权 GET 策略端点，并再次核对身份、查询及当前数据租户的一致性。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class AbacServiceAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext>,
        AuthorizationManagerProvider<RequestAuthorizationContext>, Ordered {
    static final RequestMatcher REQUEST = PathPatternRequestMatcher.withDefaults()
            .matcher(HttpMethod.GET, AbacPolicyQueryService.PATH);

    @Override
    public AuthorizationManager<RequestAuthorizationContext> get() {
        return this;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        final Authentication current = authentication.get();
        return new AuthorizationDecision(current instanceof AbacServiceAuthentication service && service.isAuthenticated()
                && REQUEST.matches(context.getRequest())
                && service.getTenant().equals(context.getRequest().getParameter("tenant"))
                && service.getTenant().equals(TenantContextHolder.getTenantId()));
    }
}
