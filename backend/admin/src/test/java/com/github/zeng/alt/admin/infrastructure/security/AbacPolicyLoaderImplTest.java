package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.entity.HttpResource;
import com.github.zeng.alt.admin.infrastructure.entity.PolicyRule;
import com.github.zeng.alt.admin.infrastructure.repository.PolicyRuleRepository;
import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import io.vavr.control.Option;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
class AbacPolicyLoaderImplTest {

    @Mock
    private PolicyRuleRepository repository;

    private final AbacPolicyMapper mapper = Mappers.getMapper(AbacPolicyMapper.class);
    private final AbacContext context = new AbacContext("t001", "user:update", Map.of(), Map.of(), Map.of());
    private AbacPolicyLoaderImpl loader;

    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();
        TenantContextHolder.setTenantId("t001");
        loader = new AbacPolicyLoaderImpl(repository, mapper);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldRejectMismatchedTenantBeforeQuerying() {
        TenantContextHolder.setTenantId("t002");

        assertThatThrownBy(() -> loader.list(context)).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectMissingCurrentTenantBeforeQuerying() {
        TenantContextHolder.clear();

        assertThatThrownBy(() -> loader.list(context)).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void shouldQueryCurrentTenantAndMapEveryPolicyInRepositoryOrder() {
        when(repository.listByPermission("t001", "user:update")).thenReturn(List.of(
                rule(11L, "resource['id'] == '42'", true, 42L),
                rule(12L, "false", false, null),
                rule(Long.MAX_VALUE, "true", null, Long.MAX_VALUE)
        ));

        assertThat(loader.list(context)).containsExactly(
                new AbacPolicy("11", "t001", "user:update", "resource['id'] == '42'", true, Option.of("42")),
                new AbacPolicy("12", "t001", "user:update", "false", false, Option.none()),
                new AbacPolicy("9223372036854775807", "t001", "user:update", "true", false, Option.of("9223372036854775807"))
        );
        verify(repository).listByPermission("t001", "user:update");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnEmptyListWhenNoPoliciesExist() {
        when(repository.listByPermission("t001", "user:update")).thenReturn(List.of());

        assertThat(loader.list(context)).isEmpty();

        verify(repository).listByPermission("t001", "user:update");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldPropagateDatabaseFailureWithoutReturningEmptyPolicies() {
        final var failure = new DataAccessResourceFailureException("database unavailable");
        when(repository.listByPermission("t001", "user:update")).thenThrow(failure);

        assertThatThrownBy(() -> loader.list(context)).isSameAs(failure);

        verify(repository).listByPermission("t001", "user:update");
        verifyNoMoreInteractions(repository);
    }

    private PolicyRule rule(Long id, String condition, Boolean preAuth, Long resourceId) {
        final var rule = new PolicyRule();
        rule.setPolicyRuleId(id);
        rule.setTenantBy("t001");
        rule.setCondition(condition);
        rule.setPreAuth(preAuth);
        rule.setResourceId(resourceId);
        final var permission = new HttpResource();
        permission.setCode("user:update");
        rule.setPermission(permission);
        return rule;
    }
}
