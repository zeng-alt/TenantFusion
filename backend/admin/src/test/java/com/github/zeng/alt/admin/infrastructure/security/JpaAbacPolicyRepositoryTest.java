package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.entity.HttpResource;
import com.github.zeng.alt.admin.infrastructure.entity.PolicyRule;
import com.github.zeng.alt.admin.infrastructure.repository.PolicyRuleRepository;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQuery;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * 策略加载的租户边界、真实 MapStruct 映射和异常传播测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@ExtendWith(MockitoExtension.class)
class JpaAbacPolicyRepositoryTest {

    @Mock
    private PolicyRuleRepository repository;

    private final AbacPolicyMapper mapper = Mappers.getMapper(AbacPolicyMapper.class);
    private final AbacPolicyQuery context = new AbacPolicyQuery("t001", "user:update");
    private JpaAbacPolicyRepository loader;

    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();
        TenantContextHolder.setTenantId("t001");
        loader = new JpaAbacPolicyRepository(repository, mapper);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldRejectMismatchedTenantBeforeQuerying() {
        TenantContextHolder.setTenantId("t002");

        assertThat(loader.list(context).getLeft().code()).isEqualTo("TENANT_MISMATCH");

        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectMissingCurrentTenantBeforeQuerying() {
        TenantContextHolder.clear();

        assertThat(loader.list(context).getLeft().code()).isEqualTo("TENANT_MISMATCH");

        verifyNoInteractions(repository);
    }

    @Test
    void shouldQueryCurrentTenantAndMapEveryPolicyInRepositoryOrder() {
        when(repository.listByPermission("t001", "user:update")).thenReturn(List.of(
                rule(11L, "resource['id'] == '42'", true),
                rule(12L, "false", false),
                rule(Long.MAX_VALUE, "true", null)
        ));

        assertThat(loader.list(context).get()).containsExactly(
                new AbacPolicy("11", "t001", "user:update", "resource['id'] == '42'", true),
                new AbacPolicy("12", "t001", "user:update", "false", false),
                new AbacPolicy("9223372036854775807", "t001", "user:update", "true", false)
        );
        verify(repository).listByPermission("t001", "user:update");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnEmptyListWhenNoPoliciesExist() {
        when(repository.listByPermission("t001", "user:update")).thenReturn(List.of());

        assertThat(loader.list(context).get()).isEmpty();

        verify(repository).listByPermission("t001", "user:update");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldPropagateDatabaseFailureWithoutReturningEmptyPolicies() {
        final var failure = new DataAccessResourceFailureException("database unavailable");
        when(repository.listByPermission("t001", "user:update")).thenThrow(failure);

        assertThat(loader.list(context).getLeft().code()).isEqualTo("POLICY_QUERY_FAILED");

        verify(repository).listByPermission("t001", "user:update");
        verifyNoMoreInteractions(repository);
    }

    private PolicyRule rule(Long id, String condition, Boolean preAuth) {
        final var rule = new PolicyRule();
        rule.setPolicyRuleId(id);
        rule.setTenantBy("t001");
        rule.setCondition(condition);
        rule.setPreAuth(preAuth);

        final var permission = new HttpResource();
        permission.setCode("user:update");
        rule.setPermission(permission);
        return rule;
    }
}
