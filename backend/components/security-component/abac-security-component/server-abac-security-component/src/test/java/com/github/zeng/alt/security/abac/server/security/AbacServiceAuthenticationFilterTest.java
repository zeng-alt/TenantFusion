package com.github.zeng.alt.security.abac.server.security;

import com.github.zeng.alt.security.abac.server.config.AbacServerProperties;
import com.github.zeng.alt.security.abac.server.config.AbacServiceClientProperties;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.tenant.api.TenantRouting;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 服务过滤器不能复用上一个租户的数据路由，正常及异常结束均恢复调用方上下文。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacServiceAuthenticationFilterTest {
    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void replacesTenantRoutingAndRestoresAllContextWhenDownstreamFails() {
        final var properties = new AbacServerProperties();
        final var account = new AbacServiceClientProperties();
        account.setKey("test-key");
        account.setTenants(Set.of("t001"));
        properties.setClients(Map.of("workflow", account));
        final var filter = new AbacServiceAuthenticationFilter(properties,
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), new AccessDeniedHandlerImpl());
        final var previous = SecurityContextHolder.createEmptyContext();
        previous.setAuthentication(new TestingAuthenticationToken("user", "unused"));
        SecurityContextHolder.setContext(previous);
        final var routing = new TenantRouting("previous", "database", "schema", null, true);
        TenantContextHolder.setTenantId("previous");
        TenantContextHolder.setDatabase("database");
        TenantContextHolder.setSchema("schema");
        TenantContextHolder.setRouting(routing);
        final var request = new MockHttpServletRequest("GET", "/internal/abac/policies");
        request.setServletPath("/internal/abac/policies");
        request.setParameter("tenant", "t001");
        request.addHeader("X-Abac-Client", "workflow");
        request.addHeader("X-Abac-Service-Key", "test-key");
        request.addHeader("X-Tenant-Id", "t001");
        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isInstanceOf(AbacServiceAuthentication.class);
            assertThat(TenantContextHolder.getTenantId()).isEqualTo("t001");
            assertThat(TenantContextHolder.getDatabase()).isNull();
            assertThat(TenantContextHolder.getSchema()).isNull();
            assertThat(TenantContextHolder.getRouting()).isNull();
            throw new ServletException("downstream failure");
        })).isInstanceOf(ServletException.class);
        assertThat(SecurityContextHolder.getContext()).isSameAs(previous);
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("previous");
        assertThat(TenantContextHolder.getDatabase()).isEqualTo("database");
        assertThat(TenantContextHolder.getSchema()).isEqualTo("schema");
        assertThat(TenantContextHolder.getRouting()).isSameAs(routing);
    }
}
