package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.security.api.AuthorizationConstraint;
import com.github.zeng.alt.security.api.WhiteListService;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 基础授权之后执行的必需约束，不参与 OR 放行。显式白名单维持原有豁免。
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
    private final WhiteListService whiteList;

    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        final var request = context.getRequest();
        if (whiteList.getWhiteList().stream()
                .anyMatch(path -> PathPatternRequestMatcher.withDefaults().matcher(path).matches(request))) {
            return new AuthorizationDecision(true);
        }
        return Try.of(() -> service.authorizeHttp(
                        authentication.get(), request.getMethod(), request.getRequestURI(),
                        routes.match(request.getMethod(), request.getRequestURI())
                ))
                .toEither().mapLeft(ignored -> "授权上下文解析失败")
                .flatMap(decision -> decision)
                .peekLeft(reason -> log.debug("ABAC request denied: {}", reason))
                .fold(reason -> new AuthorizationDecision(false), AuthorizationDecision::new);
    }
}
