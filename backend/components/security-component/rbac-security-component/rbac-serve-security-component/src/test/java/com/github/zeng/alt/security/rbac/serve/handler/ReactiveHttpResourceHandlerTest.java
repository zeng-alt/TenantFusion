package com.github.zeng.alt.security.rbac.serve.handler;

import com.github.zeng.alt.security.api.HttpResource;
import com.github.zeng.alt.security.rbac.serve.config.UnmatchedUrlPolicy;
import com.github.zeng.alt.security.rbac.serve.locator.ReactivePermissionLocator;
import com.github.zeng.alt.security.rbac.serve.locator.ReactiveResourceSignageLocator;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Reactive URL 无匹配权限模式：与 Servlet 行为一致，无匹配按模式，已配权限码始终要求持有。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ReactiveHttpResourceHandlerTest {

    private final RouteTemplateManager routes = mock(RouteTemplateManager.class);
    private final ReactivePermissionLocator permissions = mock(ReactivePermissionLocator.class);
    private final ReactiveResourceSignageLocator signage = mock(ReactiveResourceSignageLocator.class);
    private final Authentication authentication = mock(Authentication.class);

    private ReactiveHttpResourceHandler handler(UnmatchedUrlPolicy policy) {
        return new ReactiveHttpResourceHandler(routes, permissions, signage, policy);
    }

    private AuthorizationContext context() {
        final ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(URI.create("/v1/user/42"));
        when(request.getMethod()).thenReturn(HttpMethod.PATCH);
        final ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(exchange.getRequest()).thenReturn(request);
        final AuthorizationContext context = mock(AuthorizationContext.class);
        when(context.getExchange()).thenReturn(exchange);
        return context;
    }

    private void stubRequired(Mono<String> required) {
        when(routes.match("PATCH", "/v1/user/42")).thenReturn("/v1/user/{id}");
        when(signage.load(any(HttpResource.class), eq(authentication))).thenReturn(required);
    }

    @Test
    void shouldDenyUnmatchedUrlByDefault() {
        stubRequired(Mono.empty());
        StepVerifier.create(handler(UnmatchedUrlPolicy.DENY).handler(Mono.just(authentication), context()))
                .expectNext(false).verifyComplete();
    }

    @Test
    void shouldPermitUnmatchedUrlWhenConfigured() {
        stubRequired(Mono.empty());
        StepVerifier.create(handler(UnmatchedUrlPolicy.PERMIT).handler(Mono.just(authentication), context()))
                .expectNext(true).verifyComplete();
    }

    @Test
    void shouldGrantWhenUserHoldsRequiredPermission() {
        stubRequired(Mono.just("user:update"));
        when(permissions.load(authentication)).thenReturn(Mono.just(Set.of("user:update")));
        StepVerifier.create(handler(UnmatchedUrlPolicy.DENY).handler(Mono.just(authentication), context()))
                .expectNext(true).verifyComplete();
        StepVerifier.create(handler(UnmatchedUrlPolicy.PERMIT).handler(Mono.just(authentication), context()))
                .expectNext(true).verifyComplete();
    }

    @Test
    void shouldDenyWhenUserLacksRequiredPermissionRegardlessOfPolicy() {
        stubRequired(Mono.just("user:update"));
        when(permissions.load(authentication)).thenReturn(Mono.just(Set.of("other")));
        StepVerifier.create(handler(UnmatchedUrlPolicy.DENY).handler(Mono.just(authentication), context()))
                .expectNext(false).verifyComplete();
        StepVerifier.create(handler(UnmatchedUrlPolicy.PERMIT).handler(Mono.just(authentication), context()))
                .expectNext(false).verifyComplete();
    }

    @Test
    void shouldDenyWhenUserHasNoPermissionsEvenInPermitMode() {
        stubRequired(Mono.just("user:update"));
        when(permissions.load(authentication)).thenReturn(Mono.just(Set.of()));
        StepVerifier.create(handler(UnmatchedUrlPolicy.PERMIT).handler(Mono.just(authentication), context()))
                .expectNext(false).verifyComplete();
    }
}