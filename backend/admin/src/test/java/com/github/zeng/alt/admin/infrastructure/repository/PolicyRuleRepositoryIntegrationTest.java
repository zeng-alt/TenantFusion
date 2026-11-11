package com.github.zeng.alt.admin.infrastructure.repository;

import com.github.zeng.alt.admin.infrastructure.entity.Department;
import com.github.zeng.alt.admin.infrastructure.entity.DictData;
import com.github.zeng.alt.admin.infrastructure.entity.DictType;
import com.github.zeng.alt.admin.infrastructure.entity.HttpResource;
import com.github.zeng.alt.admin.infrastructure.entity.MenuResource;
import com.github.zeng.alt.admin.infrastructure.entity.Permission;
import com.github.zeng.alt.admin.infrastructure.entity.PolicyRule;
import com.github.zeng.alt.admin.infrastructure.entity.Role;
import com.github.zeng.alt.admin.infrastructure.entity.RolePermission;
import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.entity.UserResource;
import com.github.zeng.alt.admin.infrastructure.entity.UserRole;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 在 H2 上执行仓储声明的真实 JPQL，验证两侧租户过滤及软删除。
 * 禁用与查询无关的实体回调，避免启动 Redis 等外部基础设施。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class PolicyRuleRepositoryIntegrationTest {
    private static StandardServiceRegistry registry;
    private static SessionFactory factory;

    @BeforeAll
    static void setUp() {
        registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.url", "jdbc:h2:mem:abac_policy_repository;DB_CLOSE_DELAY=-1")
                .applySetting("hibernate.connection.driver_class", "org.h2.Driver")
                .applySetting("hibernate.hbm2ddl.auto", "create-drop")
                .applySetting("hibernate.jpa_callbacks.enabled", false)
                .applySetting("hibernate.physical_naming_strategy",
                        "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
                .applySetting("hibernate.tenant_identifier_resolver", new RootTenantResolver())
                .build();
        final var metadata = new MetadataSources(registry);
        List.of(Department.class, DictData.class, DictType.class, HttpResource.class,
                MenuResource.class, Permission.class, PolicyRule.class, Role.class, RolePermission.class,
                User.class, UserResource.class, UserRole.class).forEach(metadata::addAnnotatedClass);
        factory = metadata.buildMetadata().buildSessionFactory();
        factory.inTransaction(session -> {
            session.createNativeMutationQuery("""
                    insert into main_permission (permission_id, resource_type, code, tenant_by, is_enabled, is_deleted)
                    values (1, 'HTTP', 'user:update', 't001', true, false),
                           (2, 'HTTP', 'user:update', 't002', true, false),
                           (3, 'HTTP', 'disabled', 't001', false, false),
                           (4, 'HTTP', 'deleted', 't001', true, true)
                    """).executeUpdate();
            session.createNativeMutationQuery("""
                    insert into main_policy_rule
                        (policy_rule_id, permission_id, tenant_by, condition, is_pre_auth, is_deleted)
                    values (11, 1, 't001', 'true', true, false),
                           (12, 2, 't002', 'false', true, false),
                           (13, 2, 't001', 'false', true, false),
                           (14, 1, 't001', 'false', true, true),
                           (15, 3, 't001', 'true', true, false),
                           (16, 4, 't001', 'true', true, false)
                    """).executeUpdate();
        });
    }

    @AfterAll
    static void close() {
        if (factory != null) {
            factory.close();
        }
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @Test
    void shouldFilterBothTenantsEvenWithRootSession() throws ReflectiveOperationException {
        assertThat(list("t001", "user:update")).extracting(PolicyRule::getPolicyRuleId).containsExactly(11L);
        assertThat(list("t002", "user:update")).extracting(PolicyRule::getPolicyRuleId).containsExactly(12L);
    }

    @Test
    void shouldExcludeDisabledAndSoftDeletedPermissions() throws ReflectiveOperationException {
        assertThat(list("t001", "disabled")).isEmpty();
        assertThat(list("t001", "deleted")).isEmpty();
    }

    @Test
    void shouldExecuteLegacyPermissionPrimaryKeyQuery() throws ReflectiveOperationException {
        final String jpql = PolicyRuleRepository.class.getMethod("findByPermissionId", Long.class)
                .getAnnotation(Query.class).value();
        try (final var session = factory.openSession()) {
            assertThat(session.createQuery(jpql, PolicyRule.class).setParameter("permissionId", 1L).getResultList())
                    .extracting(PolicyRule::getPolicyRuleId).containsExactly(11L);
        }
    }

    private List<PolicyRule> list(String tenant, String permission) throws ReflectiveOperationException {
        final String jpql = PolicyRuleRepository.class.getMethod("listByPermission", String.class, String.class)
                .getAnnotation(Query.class).value();
        try (final var session = factory.openSession()) {
            return session.createQuery(jpql, PolicyRule.class)
                    .setParameter("tenant", tenant).setParameter("permission", permission).getResultList();
        }
    }

    static class RootTenantResolver implements CurrentTenantIdentifierResolver<String> {
        @Override
        public String resolveCurrentTenantIdentifier() {
            return "master";
        }

        @Override
        public boolean validateExistingCurrentSessions() {
            return true;
        }

        @Override
        public boolean isRoot(String tenant) {
            return "master".equals(tenant);
        }
    }
}
