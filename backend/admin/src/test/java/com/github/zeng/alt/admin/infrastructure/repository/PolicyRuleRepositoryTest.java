package com.github.zeng.alt.admin.infrastructure.repository;

import com.github.zeng.alt.admin.infrastructure.entity.Permission;
import com.github.zeng.alt.admin.infrastructure.entity.PolicyRule;
import org.hibernate.annotations.SQLRestriction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 仓储声明契约单元测试，不启动 admin，也不替代数据库层的 JPQL 执行与隔离测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class PolicyRuleRepositoryTest {

    @Test
    void shouldKeepLegacySignatureWithExplicitPermissionPrimaryKeyPath() throws NoSuchMethodException {
        final Method method = PolicyRuleRepository.class.getMethod("findByPermissionId", Long.class);
        final Query query = method.getAnnotation(Query.class);

        assertThat(method.getReturnType()).isEqualTo(List.class);
        assertThat(query).isNotNull();
        assertThat(query.nativeQuery()).isFalse();
        assertThat(query.value()).isEqualToNormalizingWhitespace("""
                select p from PolicyRule p
                where p.permission.permissionId = :permissionId
                """);
        assertThat(method.getParameters()[0].getAnnotation(Param.class)).isNotNull();
        assertThat(method.getParameters()[0].getAnnotation(Param.class).value()).isEqualTo("permissionId");
    }

    @Test
    void shouldRequireBothTenantsAndEnabledPermissionAndOrderByPolicyId() throws NoSuchMethodException {
        final Method method = PolicyRuleRepository.class.getMethod("listByPermission", String.class, String.class);
        final Query query = method.getAnnotation(Query.class);

        assertThat(method.getReturnType()).isEqualTo(List.class);
        assertThat(query).isNotNull();
        assertThat(query.nativeQuery()).isFalse();
        assertThat(query.value()).isEqualToNormalizingWhitespace("""
                select p from PolicyRule p
                join fetch p.permission perm
                where p.tenantBy = :tenant
                  and perm.tenantBy = :tenant
                  and perm.code = :permission
                  and perm.enabled = true
                order by p.policyRuleId
                """);
        assertThat(method.getParameters()[0].getAnnotation(Param.class)).isNotNull();
        assertThat(method.getParameters()[0].getAnnotation(Param.class).value()).isEqualTo("tenant");
        assertThat(method.getParameters()[1].getAnnotation(Param.class)).isNotNull();
        assertThat(method.getParameters()[1].getAnnotation(Param.class).value()).isEqualTo("permission");
    }

    @ParameterizedTest
    @ValueSource(classes = {PolicyRule.class, Permission.class})
    void shouldDeclareSoftDeleteRestrictionsOnBothEntities(Class<?> entityType) {
        final SQLRestriction restriction = entityType.getAnnotation(SQLRestriction.class);

        assertThat(restriction).isNotNull();
        assertThat(restriction.value()).isEqualTo("is_deleted=false");
    }
}
