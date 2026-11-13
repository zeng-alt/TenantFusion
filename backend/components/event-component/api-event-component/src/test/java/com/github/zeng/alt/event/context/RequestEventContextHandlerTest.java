package com.github.zeng.alt.event.context;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证持久化用户身份的恢复与线程原认证上下文的隔离、还原。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class RequestEventContextHandlerTest {

    private final RequestEventContextHandler handler = new RequestEventContextHandler();

    @AfterEach
    void clearContexts() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
    }

    @Test
    void restoresUserContextFromPersistedSnapshotWithoutCredentialsOrAuthorities() throws Exception {
        final var user = SecurityUser.withUsername("alice").id("42").password("secret").build();
        UserContextHolder.set(UsernamePasswordAuthenticationToken.authenticated(
                user, "access-token", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
        TenantContextHolder.setTenantId("origin");
        TenantContextHolder.setDatabase("database");
        TenantContextHolder.setSchema("schema");
        final var mapper = new ObjectMapper();
        final String serialized = mapper.writeValueAsString(handler.capture());
        final Map<String, String> snapshotByKey = mapper.readValue(serialized, new TypeReference<>() {});
        assertThat(serialized).doesNotContain("secret", "access-token", "ROLE_ADMIN");
        UserContextHolder.clear();
        TenantContextHolder.clear();
        try (final var scope = handler.restore(snapshotByKey)) {
            assertThat(UserContextHolder.getId()).isEqualTo("42");
            assertThat(UserContextHolder.getUsername()).isEqualTo("alice");
            assertThat(UserContextHolder.getTenant()).isEqualTo("origin");
            assertThat(UserContextHolder.getSecurityUser().getDatabase()).isEqualTo("database");
            assertThat(UserContextHolder.getSecurityUser().getSchema()).isEqualTo("schema");
            assertThat(UserContextHolder.getUserDetails().getPassword()).isNull();
            assertThat(UserContextHolder.getAuthentication().getCredentials()).isNull();
            assertThat(UserContextHolder.getAuthentication().getAuthorities()).isEmpty();
        }
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void preservesOriginalContextObjectAcrossNestedAnonymousScopeAndFailure() {
        final var worker = UsernamePasswordAuthenticationToken.unauthenticated("worker", "worker-token");
        UserContextHolder.set(worker);
        final var originalContext = SecurityContextHolder.getContext();
        assertThatThrownBy(() -> {
            try (final var outer = handler.restore(Map.of("userId", "42", "username", "alice"))) {
                assertThat(SecurityContextHolder.getContext()).isNotSameAs(originalContext);
                assertThat(originalContext.getAuthentication()).isSameAs(worker);
                assertThat(UserContextHolder.getId()).isEqualTo("42");
                final var eventContext = SecurityContextHolder.getContext();
                try (final var inner = handler.restore(Map.of())) {
                    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
                }
                assertThat(SecurityContextHolder.getContext()).isSameAs(eventContext);
                assertThat(UserContextHolder.getId()).isEqualTo("42");
                throw new IllegalStateException("handler failed");
            }
        }).hasMessage("handler failed");
        assertThat(SecurityContextHolder.getContext()).isSameAs(originalContext);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(worker);
    }

    @Test
    void doesNotTurnAnonymousSnapshotIntoAuthenticatedUser() {
        UserContextHolder.set(new AnonymousAuthenticationToken(
                "anonymous", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        ));
        final Map<String, String> snapshotByKey = handler.capture();
        final var worker = UsernamePasswordAuthenticationToken.authenticated("worker", null, List.of());
        UserContextHolder.set(worker);
        try (final var scope = handler.restore(snapshotByKey)) {
            assertThat(UserContextHolder.getAuthentication()).isNull();
        }
        assertThat(UserContextHolder.getAuthentication()).isSameAs(worker);
    }
}
