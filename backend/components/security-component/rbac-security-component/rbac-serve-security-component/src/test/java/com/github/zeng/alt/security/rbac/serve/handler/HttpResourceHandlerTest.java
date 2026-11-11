package com.github.zeng.alt.security.rbac.serve.handler;

import com.github.zeng.alt.security.api.HttpResource;
import com.github.zeng.alt.security.rbac.serve.config.UnmatchedUrlPolicy;
import com.github.zeng.alt.security.rbac.serve.locator.PermissionLocator;
import com.github.zeng.alt.security.rbac.serve.locator.ResourceSignageLocator;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * URL 无匹配权限模式：DENY/PERMIT 只作用于无权限码的 URL；已配权限码始终要求持有。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class HttpResourceHandlerTest {

    private final RouteTemplateManager routes = mock(RouteTemplateManager.class);
    private final PermissionLocator permissions = mock(PermissionLocator.class);
    private final ResourceSignageLocator signage = mock(ResourceSignageLocator.class);
    private final Authentication authentication = mock(Authentication.class);

    private HttpResourceHandler handler(UnmatchedUrlPolicy policy) {
        return new HttpResourceHandler(routes, permissions, signage, policy);
    }

    private RequestAuthorizationContext context() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/v1/user/42");
        when(request.getMethod()).thenReturn("PATCH");
        final RequestAuthorizationContext context = mock(RequestAuthorizationContext.class);
        when(context.getRequest()).thenReturn(request);
        return context;
    }

    private void stubRequired(String required) {
        when(routes.match("PATCH", "/v1/user/42")).thenReturn("/v1/user/{id}");
        when(signage.load(any(HttpResource.class), eq(authentication))).thenReturn(required);
    }

    @Test
    void shouldDenyUnmatchedUrlByDefault() {
        stubRequired(null);
        assertThat(handler(UnmatchedUrlPolicy.DENY).handler(authentication, context())).isFalse();
    }

    @Test
    void shouldPermitUnmatchedUrlWhenConfigured() {
        stubRequired(null);
        assertThat(handler(UnmatchedUrlPolicy.PERMIT).handler(authentication, context())).isTrue();
    }

    @Test
    void shouldGrantWhenUserHoldsRequiredPermission() {
        stubRequired("user:update");
        when(permissions.load(authentication)).thenReturn(Set.of("user:update"));
        assertThat(handler(UnmatchedUrlPolicy.DENY).handler(authentication, context())).isTrue();
        assertThat(handler(UnmatchedUrlPolicy.PERMIT).handler(authentication, context())).isTrue();
    }

    @Test
    void shouldDenyWhenUserLacksRequiredPermissionRegardlessOfPolicy() {
        stubRequired("user:update");
        when(permissions.load(authentication)).thenReturn(Set.of("other"));
        assertThat(handler(UnmatchedUrlPolicy.DENY).handler(authentication, context())).isFalse();
        assertThat(handler(UnmatchedUrlPolicy.PERMIT).handler(authentication, context())).isFalse();
    }

    @Test
    void shouldDenyWhenUserHasNoPermissionsEvenInPermitMode() {
        stubRequired("user:update");
        when(permissions.load(authentication)).thenReturn(Set.of());
        assertThat(handler(UnmatchedUrlPolicy.PERMIT).handler(authentication, context())).isFalse();
    }
}