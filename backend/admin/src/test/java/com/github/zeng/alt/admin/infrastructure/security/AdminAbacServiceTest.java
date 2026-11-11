package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.repository.UserRepository;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.core.AbacProperties;
import com.github.zeng.alt.security.abac.core.AbacTemplateImpl;
import com.github.zeng.alt.security.abac.method.AbacSubject;
import com.github.zeng.alt.security.abac.spel.SpelAbacEvaluator;
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
import org.mapstruct.factory.Mappers;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private AdminAbacService service;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId("t001");
        authentication = authentication("7");
        UserContextHolder.setAuthentication(authentication);
        final var template = new AbacTemplateImpl(ignored -> List.of(new AbacPolicy(
                "1", "t001", "user:update", "subject['deptId'] == resource['deptId']", true, Option.none()
        )), new SpelAbacEvaluator(), new AbacProperties());
        service = new AdminAbacService(template, rbac, users, new SecurityProperties(), Mappers.getMapper(AbacUserMapper.class));
        when(users.findById(7L)).thenReturn(Option.of(user(7L, 10L, "t001")));
    }

    @AfterEach
    void cleanContext() {
        UserContextHolder.clear();
        TenantContextHolder.clear();
    }

    @Test
    void shouldGrantSameDepartmentAndRejectDifferentDepartment() {
        when(rbac.findRolePermission(any(), eq("7"), eq("t001"))).thenReturn(Set.of("user:update"));
        assertThat(service.authorize(authentication, "user:update", attributes(10L), Map.of()).isRight()).isTrue();
        assertThat(service.authorize(authentication, "user:update", attributes(20L), Map.of()).isLeft()).isTrue();
    }

    @Test
    void shouldRejectRbacDenialWithoutLoadingSubject() {
        when(rbac.findRolePermission(any(), eq("7"), eq("t001"))).thenReturn(Set.of());
        assertThat(service.authorize(authentication, "user:update", attributes(10L), Map.of()).isLeft()).isTrue();
        verifyNoInteractions(users);
    }

    @Test
    void shouldNotLetSuperAdminBypassAbac() {
        final Authentication admin = authentication("1");
        when(users.findById(1L)).thenReturn(Option.of(user(1L, 10L, "t001")));
        assertThat(service.authorize(admin, "user:update", attributes(20L), Map.of()).isLeft()).isTrue();
        verifyNoInteractions(rbac);
    }

    @Test
    void shouldRejectMismatchedTenantBeforeQuerying() {
        TenantContextHolder.setTenantId("t002");
        assertThat(service.authorize(authentication, "user:update", attributes(10L), Map.of()).isLeft()).isTrue();
        verifyNoInteractions(users, rbac);
    }

    @Test
    void shouldRejectForeignResource() {
        assertThat(service.authorize(authentication, "user:update", Map.of("tenant", "t002"), Map.of()).isLeft()).isTrue();
        verifyNoInteractions(users, rbac);
    }

    @Test
    void shouldLoadTrustedResourceForUserPatch() {
        when(rbac.findPermission(eq("t001"), any(HttpResource.class))).thenReturn("user:update");
        when(rbac.findRolePermission(any(), eq("7"), eq("t001"))).thenReturn(Set.of("user:update"));
        when(users.findById(42L)).thenReturn(Option.of(user(42L, 20L, "t001")));
        assertThat(service.authorizeHttp(authentication, "PATCH", "/v1/user/42", "/v1/user/{id}").isLeft()).isTrue();
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

    private Authentication authentication(String id) {
        final var role = new SimpleGrantedAuthority("operator");
        final var user = SecurityUser.builder().id(id).username("user" + id).tenant("t001").roles(role).currentRole(role).build();
        return UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
    }

    private Map<String, Object> attributes(Long deptId) {
        return Map.of("id", "42", "tenant", "t001", "deptId", deptId);
    }

    private User user(Long id, Long deptId, String tenant) {
        final var user = new User();
        user.setUserId(id);
        user.setDeptId(deptId);
        user.setTenantBy(tenant);
        return user;
    }
}
