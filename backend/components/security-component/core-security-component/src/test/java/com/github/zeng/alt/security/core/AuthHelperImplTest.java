package com.github.zeng.alt.security.core;

import com.github.zeng.alt.security.api.AuthorityResolver;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 管理员及以上操作权限与内置对象身份匹配回归。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AuthHelperImplTest {

    private final SecurityProperties properties = new SecurityProperties();
    private final StaticListableBeanFactory beans = new StaticListableBeanFactory();
    private final AuthHelperImpl helper = new AuthHelperImpl(properties, beans.getBeanProvider(AuthorityResolver.class));

    AuthHelperImplTest() {
        properties.getAdmin().setCode("admin");
    }

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    @ParameterizedTest
    @CsvSource({"1, , true", "1, USER, true", "7, ADMIN, true", "7, admin, true", "7, USER, false", "7, , false"})
    void grantsAdministratorAndHigherAccess(String id, String role, boolean granted) {
        login(id, role);

        assertThat(helper.hasAdmin()).isEqualTo(granted);
    }

    @Test
    void deniesAnonymousAccess() {
        assertThat(helper.hasAdmin()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"1, ", "7, ADMIN"})
    void deniesAccessWhenAdministratorExemptionIsDisabled(String id, String role) {
        properties.getAdmin().setEnabled(false);
        login(id, role);

        assertThat(helper.hasAdmin()).isFalse();
    }

    @Test
    void doesNotGrantFromAnInactiveAdministratorRole() {
        final SimpleGrantedAuthority currentRole = new SimpleGrantedAuthority("USER");
        final SecurityUser user = SecurityUser.withUsername("review").id("7").tenant("master")
                .roles(Set.of(currentRole, new SimpleGrantedAuthority("ADMIN"))).currentRole(currentRole).build();
        UserContextHolder.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                user, null, user.getAuthorities()));

        assertThat(helper.hasAdmin()).isFalse();
    }

    @Test
    void doesNotCompareRoleCodesWithSuperAdministratorIds() {
        assertThat(helper.hasAdminRole("1")).isFalse();
    }

    @Test
    void keepsBuiltInTargetProtectionWhenExemptionIsDisabled() {
        properties.getAdmin().setEnabled(false);

        assertThat(helper.hasAdminRole("ADMIN")).isTrue();
        assertThat(helper.hasSuperUser("1")).isTrue();
        assertThat(helper.hasSuperUser(1L)).isTrue();
        assertThat(helper.hasSuperUser("7")).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"ADMIN, true", "admin, false", "ROLE_ADMIN, false", "USER, false"})
    void matchesProjectRoleCodesExactlyWithoutAddingPrefix(String role, boolean granted) {
        login("7", "ADMIN");

        assertThat(helper.hasRole(role)).isEqualTo(granted);
    }

    @Test
    void checksOnlyTheActiveRole() {
        final SimpleGrantedAuthority currentRole = new SimpleGrantedAuthority("USER");
        final SecurityUser user = SecurityUser.withUsername("review").id("7").tenant("master")
                .roles(Set.of(currentRole, new SimpleGrantedAuthority("ADMIN"))).currentRole(currentRole).build();
        UserContextHolder.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                user, null, user.getAuthorities()));

        assertThat(helper.hasAnyRole("AUDITOR", "USER")).isTrue();
        assertThat(helper.hasRole("ADMIN")).isFalse();
        assertThat(helper.hasAuthority("ADMIN")).isFalse();
    }

    @Test
    void checksGrantedRolesWhenNoRoleIsSelected() {
        final SecurityUser user = SecurityUser.withUsername("review").id("7").tenant("master")
                .roles(Set.of(new SimpleGrantedAuthority("USER"), new SimpleGrantedAuthority("AUDITOR"))).build();
        UserContextHolder.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                user, null, user.getAuthorities()));

        assertThat(helper.hasAnyRole("MANAGER", "AUDITOR")).isTrue();
        assertThat(helper.hasRole("ADMIN")).isFalse();
    }

    @Test
    void resolvesPermissionCodesBeyondGrantedRoles() {
        login("7", "USER");
        final AtomicInteger calls = new AtomicInteger();
        beans.addBean("permissions", (AuthorityResolver) authentication -> {
            assertThat(authentication).isSameAs(UserContextHolder.getAuthentication());
            calls.incrementAndGet();
            return Set.of("dict:type:update", "dict:data:update");
        });

        assertThat(helper.hasAuthority("dict:type:update")).isTrue();
        assertThat(helper.hasAnyAuthority("missing", "dict:data:update")).isTrue();
        assertThat(helper.hasAuthority("DICT:TYPE:UPDATE")).isFalse();
        assertThat(calls).hasValue(3);
    }

    @Test
    void supportsGrantedAuthoritiesWithoutPermissionContributors() {
        login("7", "USER");

        assertThat(helper.hasAuthority("USER")).isTrue();
        assertThat(helper.hasAuthority("dict:type:update")).isFalse();
    }

    @Test
    void emptyChecksDenyWithoutLoadingPermissions() {
        login("7", "USER");
        beans.addBean("permissions", (AuthorityResolver) authentication -> {
            throw new AssertionError("空校验不应加载权限");
        });

        assertThat(helper.hasAnyRole()).isFalse();
        assertThat(helper.hasAnyAuthority()).isFalse();
    }

    @Test
    void anonymousChecksDenyWithoutLoadingPermissions() {
        beans.addBean("permissions", (AuthorityResolver) authentication -> {
            throw new AssertionError("匿名用户不应加载权限");
        });

        assertThat(helper.hasRole("ADMIN")).isFalse();
        assertThat(helper.hasAnyRole("USER", "ADMIN")).isFalse();
        assertThat(helper.hasAuthority("dict:type:update")).isFalse();
        assertThat(helper.hasAnyAuthority("dict:type:update", "dict:type:delete")).isFalse();

        UserContextHolder.setAuthentication(new AnonymousAuthenticationToken(
                "anonymous", "anonymousUser", Set.of(new SimpleGrantedAuthority("ADMIN"))));
        assertThat(helper.hasRole("ADMIN")).isFalse();
        assertThat(helper.hasAuthority("ADMIN")).isFalse();
    }

    private void login(String id, String role) {
        final SimpleGrantedAuthority currentRole = role == null ? null : new SimpleGrantedAuthority(role);
        final SecurityUser user = SecurityUser.withUsername("review").id(id).tenant("master")
                .roles(currentRole == null ? Set.of() : Set.of(currentRole)).currentRole(currentRole).build();
        UserContextHolder.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                user, null, user.getAuthorities()));
    }
}
