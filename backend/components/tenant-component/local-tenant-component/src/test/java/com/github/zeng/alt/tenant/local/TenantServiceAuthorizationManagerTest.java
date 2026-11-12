package com.github.zeng.alt.tenant.local;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 服务授权管理器：只给“服务身份 × 元数据路径”放行，其余弃权（false 交给链路后续）。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
class TenantServiceAuthorizationManagerTest {

    private final TenantServiceAuthorizationManager manager =
            new TenantServiceAuthorizationManager(new TenantStoreProperties());

    @Test
    @DisplayName("服务身份访问元数据端点放行")
    void grantsServiceOnMetadataPath() {
        assertThat(check(new TenantServiceAuthentication(), "/v1/tenants")).isTrue();
        assertThat(check(new TenantServiceAuthentication(), "/v1/tenants/t001")).isTrue();
    }

    @Test
    @DisplayName("服务身份访问其它路径、普通身份访问任何路径都不放行")
    void deniesOtherwise() {
        assertThat(check(new TenantServiceAuthentication(), "/v1/user")).isFalse();
        assertThat(check(new TenantServiceAuthentication(), "/v1/tenants-evil")).isFalse();
        assertThat(check(user(), "/v1/tenants")).isFalse();
        assertThat(check(null, "/v1/tenants")).isFalse();
    }

    private boolean check(Object authentication, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        RequestAuthorizationContext context = new RequestAuthorizationContext(request);
        return manager.authorize(() -> (org.springframework.security.core.Authentication) authentication,
                context).isGranted();
    }

    private UsernamePasswordAuthenticationToken user() {
        return new UsernamePasswordAuthenticationToken("u", "p",
                List.of(new SimpleGrantedAuthority("USER")));
    }
}
