package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.security.api.AuthorizationConstraint;
import com.github.zeng.alt.security.api.WhiteListAccess;
import com.github.zeng.alt.security.core.WhiteListTemplate;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import com.github.zeng.alt.tenant.local.TenantServiceAuthorizationManager;
import com.github.zeng.alt.security.abac.server.security.AbacServiceAuthorizationManager;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 基础授权之后执行的用户 ABAC 约束；白名单、租户元数据及 ABAC 查询服务身份沿用各自授权规则。
 * 管理员由 service 在账户与租户校验通过后豁免 HTTP 权限，其他全局约束继续执行。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAbacAuthorizationConstraint implements AuthorizationConstraint<RequestAuthorizationContext> {
    private final AdminAbacService service;
    private final RouteTemplateManager routes;
    private final WhiteListTemplate whiteList;
    private final ObjectProvider<TenantServiceAuthorizationManager> tenantServices;
    private final ObjectProvider<AbacServiceAuthorizationManager> abacServices;

    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        final var request = context.getRequest();
        final WhiteListAccess access = whiteList.resolve(request);
        if (access == WhiteListAccess.PERMIT_ALL) {
            return new AuthorizationDecision(true);
        }
        if (tenantServices.orderedStream().anyMatch(manager -> manager.authorize(authentication, context).isGranted())
                || abacServices.orderedStream().anyMatch(manager -> manager.authorize(authentication, context).isGranted())) {
            return new AuthorizationDecision(true);
        }
        return Try.of(() -> access == WhiteListAccess.AUTHENTICATED
                        ? service.getAuthenticatedUser(authentication.get()).map(user -> true)
                        : service.authorizeHttp(
                                authentication.get(),
                                request.getMethod(),
                                routes.match(request.getMethod(), request.getRequestURI())
                        ))
                .toEither().mapLeft(ignored -> "授权上下文解析失败")
                .flatMap(decision -> decision)
                .peekLeft(reason -> log.debug("Request authorization denied: {}", reason))
                .fold(reason -> new AuthorizationDecision(false), AuthorizationDecision::new);
    }
}
