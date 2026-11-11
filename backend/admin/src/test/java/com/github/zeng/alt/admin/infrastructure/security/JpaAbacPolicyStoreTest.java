package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.entity.HttpResource;
import com.github.zeng.alt.admin.infrastructure.entity.Permission;
import com.github.zeng.alt.admin.infrastructure.entity.PolicyRule;
import com.github.zeng.alt.admin.infrastructure.repository.PermissionRepository;
import com.github.zeng.alt.admin.infrastructure.repository.PolicyRuleRepository;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.core.AbacPolicyCreateCmd;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import io.vavr.control.Option;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * 策略写入的租户边界、权限解析、资源 ID 转换与软删除委托测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@ExtendWith(MockitoExtension.class)
class JpaAbacPolicyStoreTest {

    @Mock
    private PolicyRuleRepository repository;
    @Mock
    private PermissionRepository permissions;

    private final AbacPolicyMapper mapper = Mappers.getMapper(AbacPolicyMapper.class);
    private final AbacPolicyCreateCmd cmd = new AbacPolicyCreateCmd(
            "t001", "user:update", "subject['deptId'] == resource['deptId']", true, Option.of("42"));
    private JpaAbacPolicyStore store;

    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();
        TenantContextHolder.setTenantId("t001");
        store = new JpaAbacPolicyStore(repository, permissions, mapper);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldRejectForeignTenantBeforeAnyStorageAccess() {
        TenantContextHolder.setTenantId("t002");

        assertThat(store.create(cmd).getLeft().code()).isEqualTo("TENANT_MISMATCH");
        assertThat(store.update(policy("11", "t001")).getLeft().code()).isEqualTo("TENANT_MISMATCH");
        assertThat(store.delete("t001", "11").getLeft().code()).isEqualTo("TENANT_MISMATCH");

        verifyNoInteractions(repository, permissions);
    }

    @Test
    void shouldRejectMissingCurrentTenant() {
        TenantContextHolder.clear();

        assertThat(store.create(cmd).getLeft().code()).isEqualTo("TENANT_MISMATCH");

        verifyNoInteractions(repository, permissions);
    }

    @Test
    void shouldCreateRuleWithResolvedPermissionAndNumericResourceId() {
        when(permissions.findByTenantAndCode("t001", "user:update")).thenReturn(Option.of(permission()));
        when(repository.save(any(PolicyRule.class))).thenAnswer(invocation -> {
            final PolicyRule rule = invocation.getArgument(0);
            rule.setPolicyRuleId(11L);
            return rule;
        });

        final var created = store.create(cmd).get();

        final var captor = ArgumentCaptor.forClass(PolicyRule.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCondition()).isEqualTo(cmd.condition());
        assertThat(captor.getValue().getTenantBy()).isEqualTo("t001");
        assertThat(captor.getValue().getPreAuth()).isTrue();
        assertThat(captor.getValue().getResourceId()).isEqualTo(42L);
        assertThat(captor.getValue().getPermission().getCode()).isEqualTo("user:update");
        assertThat(created.id()).isEqualTo("11");
        assertThat(created.tenant()).isEqualTo("t001");
        assertThat(created.permission()).isEqualTo("user:update");
        assertThat(created.condition()).isEqualTo(cmd.condition());
        assertThat(created.resourceId().get()).isEqualTo("42");
        verify(permissions).findByTenantAndCode("t001", "user:update");
        verifyNoMoreInteractions(repository, permissions);
    }

    @Test
    void shouldRejectUnknownPermission() {
        when(permissions.findByTenantAndCode("t001", "user:update")).thenReturn(Option.none());

        assertThat(store.create(cmd).getLeft().code()).isEqualTo("PERMISSION_NOT_FOUND");

        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectNonNumericResourceId() {
        when(permissions.findByTenantAndCode("t001", "user:update")).thenReturn(Option.of(permission()));
        final var invalid = new AbacPolicyCreateCmd("t001", "user:update", "true", true, Option.of("abc"));

        assertThat(store.create(invalid).getLeft().code()).isEqualTo("INVALID_RESOURCE_ID");

        verifyNoInteractions(repository);
    }

    @Test
    void shouldUpdateConditionPermissionAndResourceScopeOnManagedRule() {
        final var rule = rule(11L, "true", 1L);
        when(repository.findById(11L)).thenReturn(Option.of(rule));
        when(permissions.findByTenantAndCode("t001", "user:update")).thenReturn(Option.of(permission()));
        when(repository.save(rule)).thenReturn(rule);
        final var updated = new AbacPolicy("11", "t001", "user:update", "false", false, Option.none());

        assertThat(store.update(updated).get()).isEqualTo(updated);
        assertThat(rule.getCondition()).isEqualTo("false");
        assertThat(rule.getPreAuth()).isFalse();
        assertThat(rule.getResourceId()).isNull();

        verify(repository).findById(11L);
        verify(repository).save(rule);
        verify(permissions).findByTenantAndCode("t001", "user:update");
        verifyNoMoreInteractions(repository, permissions);
    }

    @Test
    void shouldReportMissingPolicyForUnknownOrMalformedId() {
        when(repository.findById(404L)).thenReturn(Option.none());

        assertThat(store.update(policy("404", "t001")).getLeft().code()).isEqualTo("POLICY_NOT_FOUND");
        assertThat(store.update(policy("abc", "t001")).getLeft().code()).isEqualTo("POLICY_NOT_FOUND");
        assertThat(store.delete("t001", "404").getLeft().code()).isEqualTo("POLICY_NOT_FOUND");

        verifyNoInteractions(permissions);
    }

    @Test
    void shouldRejectUpdateToUnknownPermission() {
        when(repository.findById(11L)).thenReturn(Option.of(rule(11L, "true", null)));
        when(permissions.findByTenantAndCode("t001", "user:update")).thenReturn(Option.none());

        assertThat(store.update(policy("11", "t001")).getLeft().code()).isEqualTo("PERMISSION_NOT_FOUND");

        verify(repository, never()).save(any());
    }

    @Test
    void shouldDelegateDeletionToRepositorySoftDelete() {
        final var rule = rule(11L, "true", null);
        when(repository.findById(11L)).thenReturn(Option.of(rule));

        assertThat(store.delete("t001", "11").get()).isTrue();

        verify(repository).findById(11L);
        verify(repository).delete(rule);
        verifyNoMoreInteractions(repository);
    }

    private AbacPolicy policy(String id, String tenant) {
        return new AbacPolicy(id, tenant, "user:update", "true", true, Option.none());
    }

    private Permission permission() {
        final var permission = new HttpResource();
        permission.setPermissionId(1L);
        permission.setCode("user:update");
        return permission;
    }

    private PolicyRule rule(Long id, String condition, Long resourceId) {
        final var rule = new PolicyRule();
        rule.setPolicyRuleId(id);
        rule.setTenantBy("t001");
        rule.setCondition(condition);
        rule.setResourceId(resourceId);
        rule.setPermission(permission());
        return rule;
    }
}