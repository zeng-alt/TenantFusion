package com.github.zeng.alt.admin.infrastructure.security;

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
import com.github.zeng.alt.admin.infrastructure.repository.PermissionRepository;
import com.github.zeng.alt.admin.infrastructure.repository.PolicyRuleRepository;
import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.AbacTemplate;
import com.github.zeng.alt.security.abac.server.AbacPolicyCreateCmd;
import com.github.zeng.alt.security.abac.core.AbacPolicyLoader;
import com.github.zeng.alt.security.abac.core.AbacProperties;
import com.github.zeng.alt.security.abac.core.AbacTemplateImpl;
import com.github.zeng.alt.security.abac.spel.SpelAbacEvaluator;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 在 H2 上跑真实 Spring Data 仓储代理，验证「保存 SpEL → 按租户加载 → 授权求值」闭环。
 * 刻意不引入 Spring 容器：手工事务包裹每次调用，租户切换时重开 EntityManager。
 * 各用例使用互不重叠的权限标识，避免共享库内相互污染。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacPolicyStoreIntegrationTest {
    private static StandardServiceRegistry registry;
    private static SessionFactory factory;

    @BeforeAll
    static void setUp() {
        registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.url", "jdbc:h2:mem:abac_policy_store;DB_CLOSE_DELAY=-1")
                .applySetting("hibernate.connection.driver_class", "org.h2.Driver")
                .applySetting("hibernate.hbm2ddl.auto", "create-drop")
                .applySetting("hibernate.jpa_callbacks.enabled", false)
                .applySetting("hibernate.physical_naming_strategy",
                        "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
                .applySetting("hibernate.tenant_identifier_resolver", new ContextResolver())
                .build();
        final var metadata = new MetadataSources(registry);
        List.of(Department.class, DictData.class, DictType.class, HttpResource.class,
                MenuResource.class, Permission.class, PolicyRule.class, Role.class, RolePermission.class,
                User.class, UserResource.class, UserRole.class).forEach(metadata::addAnnotatedClass);
        factory = metadata.buildMetadata().buildSessionFactory();
        factory.inTransaction(session -> session.createNativeMutationQuery("""
                insert into main_permission (permission_id, resource_type, code, tenant_by, is_enabled, is_deleted)
                values (101, 'HTTP', 'store:loop', 't001', true, false),
                       (102, 'HTTP', 'store:isolation', 't001', true, false),
                       (103, 'HTTP', 'store:isolation', 't002', true, false),
                       (104, 'HTTP', 'store:lifecycle', 't001', true, false),
                       (105, 'HTTP', 'store:validation', 't001', true, false)
                """).executeUpdate());
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
    void shouldPersistExpressionAndAuthorizeThroughTenantScopedLoad() {
        final var created = withTenant("t001", entityManager -> {
            final var fixture = fixture(entityManager);
            final var policy = fixture.store().create(new AbacPolicyCreateCmd("t001",
                "store:loop",
                "principal['deptId'] == resource['deptId']",
                true)).get();
            assertThat(policy.id()).isNotBlank();
            assertThat(policy.tenant()).isEqualTo("t001");
            assertThat(policy.permission()).isEqualTo("store:loop");
            assertThat(fixture.template().authorize(context("store:loop", 10L)).get().isRight()).isTrue();
            assertThat(fixture.template().authorize(context("store:loop", 20L)).get().getLeft().code())
                    .isEqualTo("POLICY_DENIED");
            return policy;
        });

        final String stamped = factory.fromTransaction(session -> session
                .createNativeQuery("select tenant_by from main_policy_rule where policy_rule_id = :id", String.class)
                .setParameter("id", Long.parseLong(created.id()))
                .getSingleResult());
        assertThat(stamped).isEqualTo("t001");
    }

    @Test
    void shouldIsolatePolicyWritesAndReadsBetweenTenants() {
        final var created = withTenant("t001", entityManager -> fixture(entityManager).store()
                .create(new AbacPolicyCreateCmd("t001", "store:isolation", "true", true)).get());

        withTenant("t002", entityManager -> {
            final var fixture = fixture(entityManager);
            final var t002Context = new AbacContext("t002", "store:isolation", Map.of(), Map.of(), Map.of());
            assertThat(fixture.loader().list(t002Context)).isEmpty();
            assertThat(fixture.store().update(new AbacPolicy(created.id(),
                "t001",
                "store:isolation",
                "false",
                true))
                    .getLeft().code()).isEqualTo("TENANT_MISMATCH");
            assertThat(fixture.store().update(new AbacPolicy(created.id(),
                "t002",
                "store:isolation",
                "false",
                true))
                    .getLeft().code()).isEqualTo("POLICY_NOT_FOUND");
            assertThat(fixture.store().delete("t001", created.id()).getLeft().code()).isEqualTo("TENANT_MISMATCH");
            fixture.store().create(new AbacPolicyCreateCmd("t002", "store:isolation", "false", true)).get();
            assertThat(fixture.template().authorize(t002Context).get().getLeft().code()).isEqualTo("POLICY_DENIED");
            return null;
        });

        withTenant("t001", entityManager -> {
            assertThat(fixture(entityManager).template().authorize(context("store:isolation", 10L)).get().isRight())
                    .isTrue();
            return null;
        });
    }

    @Test
    void shouldUpdateConditionAndSoftDeletePolicy() {
        final String deletedId = withTenant("t001", entityManager -> {
            final var fixture = fixture(entityManager);
            final var created = fixture.store().create(new AbacPolicyCreateCmd("t001",
                "store:lifecycle",
                "true",
                true)).get();
            assertThat(fixture.template().authorize(context("store:lifecycle", 10L)).get().isRight()).isTrue();

            final var updated = fixture.store().update(new AbacPolicy(created.id(),
                "t001",
                "store:lifecycle",
                "false",
                true)).get();
            assertThat(updated.condition()).isEqualTo("false");
            assertThat(fixture.template().authorize(context("store:lifecycle", 10L)).get().getLeft().code())
                    .isEqualTo("POLICY_DENIED");

            assertThat(fixture.store().delete("t001", created.id()).get()).isTrue();
            assertThat(fixture.loader().list(context("store:lifecycle", 10L))).isEmpty();
            assertThat(fixture.template().authorize(context("store:lifecycle", 10L)).requirePolicy().get()
                    .getLeft().code()).isEqualTo("POLICY_MISSING");
            assertThat(fixture.store().delete("t001", created.id()).getLeft().code()).isEqualTo("POLICY_NOT_FOUND");
            return created.id();
        });

        final Boolean softDeleted = factory.fromTransaction(session -> session
                .createNativeQuery("select is_deleted from main_policy_rule where policy_rule_id = :id", Boolean.class)
                .setParameter("id", Long.parseLong(deletedId))
                .getSingleResult());
        assertThat(softDeleted).isTrue();
    }

    @Test
    void shouldRejectUnknownPermissionAndMissingPolicy() {
        withTenant("t001", entityManager -> {
            final var store = fixture(entityManager).store();
            assertThat(store.create(new AbacPolicyCreateCmd("t001", "store:ghost", "true", true))
                    .getLeft().code()).isEqualTo("PERMISSION_NOT_FOUND");
            assertThat(store.update(new AbacPolicy("999999", "t001", "store:validation", "true", true))
                    .getLeft().code()).isEqualTo("POLICY_NOT_FOUND");
            return null;
        });
    }

    private <T> T withTenant(String tenant, Function<EntityManager, T> work) {
        TenantContextHolder.setTenantId(tenant);
        try (final var entityManager = factory.createEntityManager()) {
            entityManager.getTransaction().begin();
            final T result = work.apply(entityManager);
            entityManager.getTransaction().commit();
            return result;
        } finally {
            TenantContextHolder.clear();
        }
    }

    private Fixture fixture(EntityManager entityManager) {
        final var repositories = new JpaRepositoryFactory(entityManager);
        final var rules = repositories.getRepository(PolicyRuleRepository.class);
        final var mapper = Mappers.getMapper(AbacPolicyMapper.class);
        final var repository = new JpaAbacPolicyRepository(rules, mapper);
        final AbacPolicyLoader loader = context -> repository.list(
                new com.github.zeng.alt.security.abac.policy.AbacPolicyQuery(context.tenant(), context.permission()))
                .getOrElseThrow(failure -> new IllegalStateException(failure.message()));
        return new Fixture(
                new JpaAbacPolicyStore(rules, repositories.getRepository(PermissionRepository.class), mapper),
                loader,
                new AbacTemplateImpl(loader, new SpelAbacEvaluator(), new AbacProperties()));
    }

    private AbacContext context(String permission, Long subjectDeptId) {
        return new AbacContext("t001", permission, Map.of("deptId", subjectDeptId), Map.of("deptId", 10L), Map.of());
    }

    private record Fixture(JpaAbacPolicyStore store, AbacPolicyLoader loader, AbacTemplate template) {
    }

    static class ContextResolver implements CurrentTenantIdentifierResolver<String> {
        @Override
        public String resolveCurrentTenantIdentifier() {
            final String tenant = TenantContextHolder.getTenantId();
            return tenant == null ? "master" : tenant;
        }

        @Override
        public boolean validateExistingCurrentSessions() {
            return false;
        }

        @Override
        public boolean isRoot(String tenant) {
            return false;
        }
    }
}
