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
import com.github.zeng.alt.admin.query.service.MenuResourceServiceImpl;
import com.github.zeng.alt.admin.query.service.transformation.MenuResourceDtoTransformation;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.security.core.TenantSwitchTemplate;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.jwt.JwtAuthenticationFilter;
import com.github.zeng.alt.security.jwt.JwtProperties;
import com.github.zeng.alt.security.jwt.JwtStorage;
import com.github.zeng.alt.security.jwt.JwtTokenProvider;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.tenant.api.TenantMode;
import com.github.zeng.alt.tenant.api.TenantRoutingRegistry;
import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import com.github.zeng.alt.tenant.core.TenantProperties;
import com.github.zeng.alt.tenant.hybrid.HybridTenantRoutingRegistry;
import com.github.zeng.alt.tenant.row.DefaultTenantDiscriminatorPolicy;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UserDetailsService;
import io.vavr.control.Option;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证应用配置下菜单仓储对各租户的自动隔离。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class MenuTenantIsolationTest {
    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod"})
    void menuQueriesOnlyReturnCurrentTenant(String profile) throws Exception {
        final var environment = new StandardEnvironment();
        new YamlPropertySourceLoader().load(profile, new ClassPathResource("application-" + profile + ".yml"))
                .forEach(environment.getPropertySources()::addFirst);
        final TenantProperties properties = Binder.get(environment)
                .bind("alt.tenant", Bindable.of(TenantProperties.class)).get();
        assertThat(properties.getEnabled()).isTrue();
        assertThat(properties.getMode()).isEqualTo(TenantMode.ROW);
        final var tenantMetadata = mock(TenantMetadataProvider.class);
        when(tenantMetadata.findById(anyString())).thenAnswer(invocation -> Optional.of(new TenantMetadata(
                invocation.getArgument(0), "租户", TenantMode.ROW, null, null, null, true, true)));
        @SuppressWarnings("unchecked")
        final ObjectProvider<TenantMetadataProvider> tenantProviders = mock(ObjectProvider.class);
        when(tenantProviders.getIfAvailable()).thenReturn(tenantMetadata);
        final TenantRoutingRegistry routing = new HybridTenantRoutingRegistry(properties, tenantProviders);
        final var policy = new DefaultTenantDiscriminatorPolicy(properties, routing);
        final var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.url", "jdbc:h2:mem:menu_tenant_" + profile + ";DB_CLOSE_DELAY=-1")
                .applySetting("hibernate.connection.driver_class", "org.h2.Driver")
                .applySetting("hibernate.hbm2ddl.auto", "create-drop")
                .applySetting("hibernate.jpa_callbacks.enabled", false)
                .applySetting("hibernate.physical_naming_strategy",
                        "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
                .applySetting("hibernate.tenant_identifier_resolver", new TenantResolver(policy))
                .build();
        try {
            final var metadata = new MetadataSources(registry);
            List.of(Department.class, DictData.class, DictType.class, HttpResource.class,
                    MenuResource.class, Permission.class, PolicyRule.class, Role.class, RolePermission.class,
                    User.class, UserResource.class, UserRole.class).forEach(metadata::addAnnotatedClass);
            try (SessionFactory factory = metadata.buildMetadata().buildSessionFactory()) {
                seedMenus(factory);
                assertMenus(factory, "master", 1L);
                assertMenus(factory, "t001", 2L, 3L);
                assertSwitchedMenu(factory);
            }
        } finally {
            TenantContextHolder.clear();
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    private void assertSwitchedMenu(SessionFactory factory) throws Exception {
        final var security = new SecurityProperties();
        security.getAdmin().setTenant("master");
        final var metadata = mock(TenantMetadataProvider.class);
        when(metadata.findCurrentById("t001")).thenReturn(Optional.of(
                new TenantMetadata("t001", "租户一", null, null, null, null, null, true)));
        @SuppressWarnings("unchecked")
        final ObjectProvider<TenantMetadataProvider> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(metadata);
        final var jwtProperties = new JwtProperties();
        final byte[] secret = new byte[192];
        new SecureRandom().nextBytes(secret);
        jwtProperties.setBase64Secret(Base64.getEncoder().encodeToString(secret));
        final var tokens = new JwtTokenProvider(jwtProperties);
        final var storage = mock(JwtStorage.class);
        when(storage.hasToken(anyString())).thenReturn(true);
        final var user = SecurityUser.withUsername("admin").id("1").tenant("master").password("unused").roles().build();
        final UserDetailsService users = username -> user;
        final var request = new MockHttpServletRequest("GET", "/v1/menu/resource/tree");
        request.setServletPath("/v1/menu/resource/tree");
        request.addHeader("Authorization", "Bearer " + tokens.createToken(user));
        request.addHeader("X-Active-Tenant", "t001");
        final var response = new MockHttpServletResponse();
        final var filter = new JwtAuthenticationFilter(tokens, storage, Option.some(users), jwtProperties,
                Option.some(new TenantSwitchTemplate(security, provider)));
        filter.doFilter(request, response, (req, res) -> {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo("t001");
            assertMenus(factory, "t001", 2L, 3L);
        });
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private void seedMenus(SessionFactory factory) {
        factory.inTransaction(session -> {
            session.createNativeMutationQuery("""
                    insert into main_permission (permission_id, resource_type, code, name, tenant_by, is_enabled, is_deleted)
                    values (1, 'MENU', 'system', '系统管理', 'master', true, false),
                           (2, 'MENU', 'system', '系统管理', 't001', true, false),
                           (3, 'MENU', 'tenant-only', '租户菜单', 't001', true, false)
                    """).executeUpdate();
            session.createNativeMutationQuery("""
                    insert into main_role (role_id, code, tenant_by)
                    values (1, 'EDITOR', 'master'), (2, 'EDITOR', 't001')
                    """).executeUpdate();
            session.createNativeMutationQuery("""
                    insert into main_role_permission (role_permission_id, role_id, permission_id, tenant_by, is_deleted)
                    values (1, 1, 1, 'master', false), (2, 2, 2, 't001', false),
                           (3, 2, 3, 't001', false)
                    """).executeUpdate();
        });
    }

    private void assertMenus(SessionFactory factory, String tenant, Long... expected) {
        TenantContextHolder.setTenantId(tenant);
        factory.inTransaction(session -> {
            final var repositories = new JpaRepositoryFactory(session);
            final var repository = repositories.getRepository(PermissionRepository.class);
            assertThat(repository.findAllPermissions()).extracting(Permission::getPermissionId)
                    .containsExactlyInAnyOrder(expected);
            assertThat(repository.findPermissionsByRoleCode("EDITOR")).extracting(Permission::getPermissionId)
                    .containsExactlyInAnyOrder(expected);
            final var properties = new SecurityProperties();
            properties.getAdmin().setCode("ADMIN");
            final var service = new MenuResourceServiceImpl(repositories.getRepository(MenuResourceRepository.class),
                    repository, mock(MenuResourceDtoTransformation.class), properties);
            assertThat(service.tree(tenant.equals("master") ? "1" : "2001", "ADMIN"))
                    .extracting(menu -> menu.getId())
                    .containsExactlyInAnyOrder(expected);
            assertThat(service.tree("2", "EDITOR")).extracting(menu -> menu.getId())
                    .containsExactlyInAnyOrder(expected);
        });
    }

    private record TenantResolver(DefaultTenantDiscriminatorPolicy policy)
            implements CurrentTenantIdentifierResolver<String> {
        @Override
        public String resolveCurrentTenantIdentifier() {
            return TenantContextHolder.getTenantId() == null ? "master" : TenantContextHolder.getTenantId();
        }

        @Override
        public boolean validateExistingCurrentSessions() {
            return false;
        }

        @Override
        public boolean isRoot(String tenantId) {
            return policy.isRoot(tenantId);
        }
    }
}
