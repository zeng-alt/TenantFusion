package com.github.zeng.alt.tenant.local;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 服务密钥过滤器：只认“元数据路径 × 正确密钥”，其余一律透传不断言。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
class TenantServiceKeyFilterTest {

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("路径与密钥都对时放入服务身份")
    void setsServiceAuthenticationOnMatch() throws Exception {
        TenantServiceKeyFilter filter = filter("s3cr3t");

        doFilter(filter, "/v1/tenants", "s3cr3t");

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isInstanceOf(TenantServiceAuthentication.class);
    }

    @Test
    @DisplayName("密钥不对、路径不对、已有身份时均不碰上下文")
    void leavesContextAloneOtherwise() throws Exception {
        TenantServiceKeyFilter filter = filter("s3cr3t");

        doFilter(filter, "/v1/tenants", "wrong");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        doFilter(filter, "/v1/user", "s3cr3t");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u", "p",
                        List.of(new SimpleGrantedAuthority("USER"))));
        doFilter(filter, "/v1/tenants", "s3cr3t");
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNotInstanceOf(TenantServiceAuthentication.class);
    }

    @Test
    @DisplayName("未配置密钥时整个通道等于不存在")
    void disabledWithoutKey() throws Exception {
        TenantServiceKeyFilter filter = filter(null);

        doFilter(filter, "/v1/tenants", "anything");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private TenantServiceKeyFilter filter(String serviceKey) {
        TenantStoreProperties properties = new TenantStoreProperties();
        properties.setServiceKey(serviceKey);
        return new TenantServiceKeyFilter(properties);
    }

    private void doFilter(TenantServiceKeyFilter filter, String uri, String key) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        if (key != null) {
            request.addHeader(TenantServiceKeyFilter.SERVICE_KEY_HEADER, key);
        }
        HttpServletRequest req = request;
        HttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();
        filter.doFilter(req, res, chain);
    }
}
