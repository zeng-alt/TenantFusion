package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.query.api.dto.AbacPolicyCmd;
import com.github.zeng.alt.admin.query.api.dto.AbacPermissionVO;
import com.github.zeng.alt.security.abac.method.AbacSchemaDto;
import com.github.zeng.alt.security.abac.method.AbacMethodSchemaDto;
import com.github.zeng.alt.security.abac.method.AbacFieldDto;
import com.github.zeng.alt.security.abac.client.cache.CachedAbacPolicyLoader;
import com.github.zeng.alt.security.abac.server.AbacPolicyService;
import com.github.zeng.alt.security.api.AuthHelper;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import io.vavr.control.Either;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 工作台写入使用真实身份租户，管理员豁免不跨越租户边界。
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacWorkbenchServiceTest {
    private final AbacWorkbenchRepository repository = mock(AbacWorkbenchRepository.class);
    private final AbacWorkbenchTemplate template = mock(AbacWorkbenchTemplate.class);
    private final AbacPolicyService policies = mock(AbacPolicyService.class);
    private final CachedAbacPolicyLoader cache = mock(CachedAbacPolicyLoader.class);
    private final AuthHelper auth = mock(AuthHelper.class);
    private final AbacWorkbenchService service = new AbacWorkbenchService(repository, template, policies, cache, auth);

    @BeforeEach
    void authenticate() {
        final SecurityUser user = SecurityUser.builder().id("7").username("editor").tenant("t001").build();
        UserContextHolder.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
        TenantContextHolder.setTenantId("t001");
    }

    @AfterEach
    void clear() {
        UserContextHolder.clear();
        TenantContextHolder.clear();
    }

    @Test
    void rejectsMissingAuthorityBeforeReadingPolicies() {
        assertThatThrownBy(() -> service.listPolicies("user:update")).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(repository, policies, template, cache);
    }

    @Test
    void adminStillCannotReadAnotherTenant() {
        TenantContextHolder.setTenantId("t002");
        when(auth.hasAdmin()).thenReturn(true);
        assertThatThrownBy(service::listPermissions).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void ordinaryManagerCanOnlyReadCurrentTenant() {
        when(auth.hasAuthority("abac:policies:read")).thenReturn(true);
        when(repository.listPolicies("t001", "user:update")).thenReturn(List.of());
        assertThat(service.listPolicies("user:update")).isEmpty();
        verify(repository).listPolicies("t001", "user:update");
    }

    @Test
    void onlyExposesPermissionsBoundToAbacMethods() {
        when(auth.hasAdmin()).thenReturn(true);
        final var bound = new AbacPermissionVO("1", "user:update", "修改用户");
        final var unbound = new AbacPermissionVO("2", "log:read", "读取日志");
        when(repository.listPermissions("t001")).thenReturn(List.of(bound, unbound));
        final var method = new AbacMethodSchemaDto("Users.update", true, List.of(),
                new AbacFieldDto("returnObject", "Long", false, "方法返回值", List.of()));
        when(template.schema("user:update"))
                .thenReturn(new AbacSchemaDto("user:update", List.of(), List.of(method), List.of()));
        when(template.schema("log:read"))
                .thenReturn(new AbacSchemaDto("log:read", List.of(), List.of(), List.of()));
        assertThat(service.listPermissions()).containsExactly(bound);
    }

    @Test
    void refusesMetadataForPermissionOutsideCurrentTenant() {
        when(auth.hasAdmin()).thenReturn(true);
        assertThatThrownBy(() -> service.schema("foreign"))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(template);
    }

    @Test
    void invalidExpressionNeverReachesPolicyWriteOrCache() {
        when(auth.hasAdmin()).thenReturn(true);
        final AbacPolicyCmd command = new AbacPolicyCmd("user:update", "#missing == 1", true);
        when(template.validate(command)).thenReturn(Either.left(
                new com.github.zeng.alt.security.abac.AbacFailure("UNKNOWN_ATTRIBUTE", "未知变量")));
        assertThat(service.create(command).getLeft().code()).isEqualTo("UNKNOWN_ATTRIBUTE");
        verifyNoInteractions(policies, cache);
    }

    @Test
    void updatePreservesServerTenantAndPhase() {
        when(auth.hasAdmin()).thenReturn(true);
        final AbacPolicyCmd command = new AbacPolicyCmd("user:update", "returnObject == #id", false);
        when(template.validate(command)).thenReturn(Either.right(true));
        when(policies.update(any())).thenAnswer(invocation -> Either.right(invocation.getArgument(0)));
        final var updated = service.update("9", command).get();
        assertThat(updated.tenant()).isEqualTo("t001");
        assertThat(updated.preAuth()).isFalse();
    }
}
