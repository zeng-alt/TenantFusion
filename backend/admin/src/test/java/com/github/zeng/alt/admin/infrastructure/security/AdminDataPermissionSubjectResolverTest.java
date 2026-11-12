package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.security.abac.method.AbacSubject;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import io.vavr.control.Either;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证有效角色必须同时拥有权限且仍属于用户，当前角色不能借用其他角色的数据范围。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AdminDataPermissionSubjectResolverTest {
    private final AdminAbacService abac = mock(AdminAbacService.class);
    private final RbacResourceService rbac = mock(RbacResourceService.class);
    private final SecurityProperties properties = new SecurityProperties();
    private final AdminDataPermissionSubjectResolver resolver = new AdminDataPermissionSubjectResolver(
            abac, rbac, properties);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldIntersectRolesMembershipAndPermissionForUnionMode() {
        authenticate(false);
        assertThat(resolver.resolve("read").get().roles()).containsExactly("reader");
        assertThat(resolver.resolve("read").get().departmentId().get()).isEqualTo(10L);
    }

    @Test
    void shouldHonorSelectedRoleInsteadOfUnioningOtherRoles() {
        authenticate(true);
        assertThat(resolver.resolve("read").get().roles()).isEmpty();
    }

    @Test
    void shouldPreserveAuthenticationAndBasePermissionRejection() {
        when(abac.resolve("read")).thenReturn(Either.left("denied"));
        assertThat(resolver.resolve("read").getLeft()).isEqualTo("denied");
    }

    private void authenticate(boolean selected) {
        final var reader = new SimpleGrantedAuthority("reader");
        final var unrelated = new SimpleGrantedAuthority("unrelated");
        final var user = SecurityUser.withUsername("reader").id("7").tenant("t1").password("unused")
                .roles(reader, unrelated, new SimpleGrantedAuthority("removed"))
                .currentRole(selected ? unrelated : null).build();
        UserContextHolder.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        when(abac.resolve("read")).thenReturn(Either.right(new AbacSubject("t1", Map.of("id", "7", "deptId", 10L))));
        when(rbac.findUserRole("7", "t1")).thenReturn(Set.of("reader", "unrelated"));
        when(rbac.findRolePermission(List.of("reader"), "7", "t1")).thenReturn(Set.of("read"));
        when(rbac.findRolePermission(List.of("unrelated"), "7", "t1")).thenReturn(Set.of("other"));
    }
}
