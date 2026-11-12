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
import com.github.zeng.alt.admin.infrastructure.repository.UserRepository;
import com.github.zeng.alt.admin.query.service.UserQueryRoleResolver;
import com.github.zeng.alt.admin.query.service.UserQueryServiceImpl;
import com.github.zeng.alt.admin.query.api.dto.UserPageQuery;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.admin.query.service.transformation.UserQueryMapper;
import com.github.zeng.alt.security.data.DataPermissionQuery;
import com.github.zeng.alt.security.data.DataPermissionTemplate;
import com.github.zeng.alt.security.data.core.DataPermissionProperties;
import com.github.zeng.alt.security.data.jpa.DataPermissionJpaTemplate;
import io.vavr.control.Either;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 在禁用 Hibernate 字节码提供器时复现原生约束，保护用户角色加载及分页映射。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class UserNativeQueryIntegrationTest {
    private static StandardServiceRegistry registry;
    private static SessionFactory factory;

    @BeforeAll
    static void setUp() {
        registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.url", "jdbc:h2:mem:user_native_query;DB_CLOSE_DELAY=-1")
                .applySetting("hibernate.hbm2ddl.auto", "create-drop")
                .applySetting("hibernate.bytecode.provider", "none")
                .applySetting("hibernate.jpa_callbacks.enabled", false)
                .applySetting("hibernate.physical_naming_strategy",
                        "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
                .applySetting("hibernate.tenant_identifier_resolver", new TenantResolver()).build();
        final var metadata = new MetadataSources(registry);
        List.of(Department.class, DictData.class, DictType.class, HttpResource.class, MenuResource.class,
                Permission.class, PolicyRule.class, Role.class, RolePermission.class, User.class,
                UserResource.class, UserRole.class).forEach(metadata::addAnnotatedClass);
        factory = metadata.buildMetadata().buildSessionFactory();
        factory.inTransaction(session -> {
            session.createNativeMutationQuery("""
                    insert into main_user (user_id, username, password, tenant_by, is_enabled, is_deleted)
                    values (901, 'native-one', 'unused', 'master', true, false),
                           (902, 'native-two', 'unused', 'master', true, false)
                    """).executeUpdate();
            session.createNativeMutationQuery("""
                    insert into main_role (role_id, code, name, tenant_by, is_enabled)
                    values (991, 'native-role', 'native role', 'master', true)
                    """).executeUpdate();
            session.createNativeMutationQuery("""
                    insert into main_user_role (user_role_id, user_id, role_id, tenant_by)
                    values (981, 901, 991, 'master'), (982, 902, 991, 'master')
                    """).executeUpdate();
        });
    }

    @AfterAll
    static void close() {
        factory.close();
        StandardServiceRegistryBuilder.destroy(registry);
    }

    @Test
    void shouldLoadAuthenticationRolesWithoutRuntimeProxyGeneration() {
        try (final var session = factory.withOptions().tenantIdentifier("master").openSession()) {
            final var repository = new JpaRepositoryFactory(session).getRepository(UserRepository.class);
            final var user = repository.findByUsername("native-one").orElseThrow();
            assertThat(user.getUserRoles()).extracting(relation -> relation.getRole().getCode())
                    .containsExactly("native-role");
            session.clear();
            assertThat(repository.findById(901L).get().getUserRoles())
                    .extracting(relation -> relation.getRole().getCode()).containsExactly("native-role");
        }
    }

    @Test
    void shouldMapPaginatedPeerRolesWithoutRuntimeProxies() {
        try (final var session = factory.withOptions().tenantIdentifier("master").openSession()) {
            final var properties = new DataPermissionProperties().resource("user", User.class,
                    fields -> fields.setTenant("tenantBy").setOwner("userId").setDepartment("deptId"));
            final DataPermissionTemplate permissions = r -> p -> () -> Either.right(
                    new DataPermissionQuery("master", "901", true, false, Set.of()));
            final var template = new DataPermissionJpaTemplate(session, permissions, properties);
            TenantContextHolder.setTenantId("master");
            final var service = new UserQueryServiceImpl(template, Mappers.getMapper(UserQueryMapper.class),
                    session, new UserQueryRoleResolver(session));
            final var page = service.page(new UserPageQuery().setPageSize(1)).get();
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().getFirst().getUserRoles().getFirst().getRole().getCode())
                    .isEqualTo("native-role");
        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * 显式租户测试上下文。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    private static class TenantResolver implements CurrentTenantIdentifierResolver<String> {
        @Override
        public String resolveCurrentTenantIdentifier() {
            return "master";
        }
        @Override
        public boolean validateExistingCurrentSessions() {
            return true;
        }
    }
}
