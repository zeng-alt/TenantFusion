package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.repository.UserRepository;
import com.github.zeng.alt.security.abac.method.AbacSubject;
import com.github.zeng.alt.security.api.HttpResource;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 真实用户属性、RBAC 与 ABAC 串联测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AdminAbacServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final RbacResourceService rbac = mock(RbacResourceService.class);
    private final SecurityProperties properties = new SecurityProperties();
    private AdminAbacService service;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId("t001");
        authentication = authentication("7");
        UserContextHolder.setAuthentication(authentication);
        service = new AdminAbacService(rbac, users, properties, Mappers.getMapper(AbacUserMapper.class));
        when(users.findById(7L)).thenReturn(Option.of(user(7L, 10L, "t001")));
    }

    @AfterEach
    void cleanContext() {
        UserContextHolder.clear();
        TenantContextHolder.clear();
    }

    @Test
    void shouldRejectMismatchedTenantBeforeQuerying() {
        TenantContextHolder.setTenantId("t002");
        assertThat(service.resolve("user:update").isLeft()).isTrue();
        verifyNoInteractions(users, rbac);
    }

    @Test
    void httpAuthorizationUsesRbacWithoutLoadingAbacAttributes() {
        when(rbac.findPermission(eq("t001"), any(HttpResource.class))).thenReturn("user:update");
        when(rbac.findRolePermission(any(), eq("7"), eq("t001"))).thenReturn(Set.of("user:update"));
        assertThat(service.authorizeHttp(authentication, "PATCH", "/v1/user/{id}")).isEqualTo(Either.right(true));
        verifyNoInteractions(users);
    }

    @Test
    void adminSubjectIsResolvedForMethodAuthorization() {
        UserContextHolder.setAuthentication(authentication("1"));
        when(users.findById(1L)).thenReturn(Option.of(user(1L, 10L, "t001")));
        assertThat(service.resolve("user:update").get().attributes()).containsEntry("deptId", 10L);
        verifyNoInteractions(rbac);
    }

    @Test
    void shouldResolveTrustedSubjectWhenRbacGrants() {
        when(rbac.findRolePermission(any(), eq("7"), eq("t001"))).thenReturn(Set.of("user:update"));
        final Either<String, AbacSubject> subject = service.resolve("user:update");
        assertThat(subject.isRight()).isTrue();
        assertThat(subject.get().tenant()).isEqualTo("t001");
        assertThat(subject.get().attributes().get("deptId")).isEqualTo(10L);
    }

    @Test
    void shouldRejectSubjectWhenRbacDenies() {
        when(rbac.findRolePermission(any(), eq("7"), eq("t001"))).thenReturn(Set.of());
        assertThat(service.resolve("user:update").isLeft()).isTrue();
    }

    @Test
    void shouldValidateLoginWhitelistPrincipalWithoutLoadingPermissionsOrAttributes() {
        assertThat(service.getAuthenticatedUser(authentication).get().getId()).isEqualTo("7");
        verifyNoInteractions(users, rbac);
    }

    @Test
    void shouldRejectLoginWhitelistPrincipalFromAnotherTenant() {
        TenantContextHolder.setTenantId("t002");
        assertThat(service.getAuthenticatedUser(authentication).getLeft()).isEqualTo("认证租户与当前数据租户不一致");
        verifyNoInteractions(users, rbac);
    }

    @Test
    void shouldRejectDisabledLoginWhitelistPrincipal() {
        final var user = (SecurityUser) authentication.getPrincipal();
        user.setEnabled(false);
        assertThat(service.getAuthenticatedUser(authentication).getLeft()).isEqualTo("当前用户未登录或已禁用");
        verifyNoInteractions(users, rbac);
    }

    @ParameterizedTest
    @CsvSource({"1, operator", "7, superAdmin", "7, SUPERADMIN"})
    void shouldExemptHttpAdminBeforeLookingUpPermissionsOrPolicies(String id, String role) {
        assertThat(service.authorizeHttp(authentication(id, role), "GET", "/v1/log/oper/list"))
                .isEqualTo(Either.right(true));
        verifyNoInteractions(users, rbac);
    }

    @Test
    void shouldRejectDisabledHttpAdminBeforeExemption() {
        final Authentication admin = authentication("1");
        ((SecurityUser) admin.getPrincipal()).setEnabled(false);
        assertThat(service.authorizeHttp(admin, "GET", null).getLeft())
                .isEqualTo("当前用户未登录或已禁用");
        verifyNoInteractions(users, rbac);
    }

    @Test
    void shouldRejectHttpAdminWhenTenantDoesNotMatch() {
        final Authentication admin = authentication("1");
        TenantContextHolder.setTenantId("t002");
        assertThat(service.authorizeHttp(admin, "GET", null).getLeft())
                .isEqualTo("认证租户与当前数据租户不一致");
        verifyNoInteractions(users, rbac);
    }

    @Test
    void shouldRespectDisabledHttpAdminExemption() {
        properties.getAdmin().setEnabled(false);
        assertThat(service.authorizeHttp(authentication("1"), "GET", "/v1/log/oper/list")
                .getLeft()).isEqualTo("未配置请求权限");
        verify(rbac).findPermission(eq("t001"), any(HttpResource.class));
        verifyNoInteractions(users);
    }

    @Test
    void shouldStillRejectOrdinaryUserWhenHttpResourceIsMissing() {
        assertThat(service.authorizeHttp(authentication, "GET", "/v1/log/oper/list")
                .getLeft()).isEqualTo("未配置请求权限");
        verify(rbac).findPermission(eq("t001"), any(HttpResource.class));
        verifyNoInteractions(users);
    }

    private Authentication authentication(String id) {
        return authentication(id, "operator");
    }

    private Authentication authentication(String id, String roleCode) {
        final var role = new SimpleGrantedAuthority(roleCode);
        final var user = SecurityUser.builder().id(id).username("user" + id).tenant("t001")
                .roles(role).currentRole(role).build();
        return UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
    }

    private User user(Long id, Long deptId, String tenant) {
        final var user = new User();
        user.setUserId(id);
        user.setDeptId(deptId);
        user.setTenantBy(tenant);
        return user;
    }
}
