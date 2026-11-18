package com.github.zeng.alt.security.data.jpa;

import com.github.zeng.alt.security.data.DataPermissionQuery;
import com.github.zeng.alt.security.data.DataPermissionTemplate;
import com.github.zeng.alt.security.data.core.DataPermissionProperties;
import com.querydsl.core.types.dsl.PathBuilder;
import io.vavr.control.Either;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 通过真实 H2 查询验证权限在分页和计数之前生效。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class DataPermissionJpaTemplateTest {
    private static StandardServiceRegistry registry;
    private static SessionFactory factory;
    private EntityManager entityManager;
    private DataPermissionJpaTemplate template;

    @BeforeAll
    static void startDatabase() {
        registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.url", "jdbc:h2:mem:data_permission;DB_CLOSE_DELAY=-1")
                .applySetting("hibernate.hbm2ddl.auto", "create-drop").build();
        factory = new MetadataSources(registry).addAnnotatedClass(Account.class).buildMetadata().buildSessionFactory();
        factory.inTransaction(session -> {
            session.persist(new Account(1L, "t1", 7L, 10L, "own"));
            session.persist(new Account(2L, "t1", 8L, 20L, "department"));
            session.persist(new Account(3L, "t1", 9L, 30L, "hidden"));
            session.persist(new Account(4L, "t2", 7L, 20L, "other-tenant"));
        });
    }

    @AfterAll
    static void stopDatabase() {
        factory.close();
        StandardServiceRegistryBuilder.destroy(registry);
    }

    @BeforeEach
    void setUp() {
        entityManager = factory.createEntityManager();
        template = template(new DataPermissionQuery("t1", "7", false, true, Set.of(20L)));
    }

    @AfterEach
    void closeSession() {
        entityManager.close();
    }

    @Test
    void shouldFilterQuerydslBeforePaginationAndCount() {
        final var account = new PathBuilder<>(Account.class, "account");
        final var query = template.query(Account.class).permission("read")
                .where(account.getString("name").ne("not-present"));
        final var page = query.page(PageRequest.of(0, 1)).get();
        assertThat(page.getContent()).extracting(a -> a.id).containsExactly(1L);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(query.count().get()).isEqualTo(2);
        assertThat(query.getDetail(3L).get()).isEmpty();
        assertThat(query.getDetail(4L).get()).isEmpty();
    }

    @Test
    void shouldIntersectSpecificationOrWithPermissionInsteadOfWideningIt() {
        final Specification<Account> anyTenant = (root, criteria, cb) -> cb.or(
                cb.equal(root.get("name"), "hidden"), cb.equal(root.get("name"), "other-tenant"));
        final var query = template.query(Account.class).permission("read").where(anyTenant);
        assertThat(query.page(PageRequest.of(0, 10)).get().getTotalElements()).isZero();
        assertThat(query.exists().get()).isFalse();
    }

    @Test
    void shouldKeepAllWithinTenantAndReturnEmptyForMissingGrants() {
        final var all = template(new DataPermissionQuery("t1", "7", true, false, Set.of()));
        assertThat(all.query(Account.class).permission("read").count().get()).isEqualTo(3);
        final var none = template(new DataPermissionQuery("t1", "7", false, false, Set.of()));
        assertThat(none.query(Account.class).permission("read").count().get()).isZero();
    }

    @Test
    void shouldNotTurnAuthorizationFailureIntoAnUnfilteredQuery() {
        final DataPermissionTemplate denied = resource -> permission -> () -> Either.left("denied");
        final var secured = new DataPermissionJpaTemplate(entityManager, denied, properties());
        assertThat(secured.query(Account.class).permission("read").count().getLeft()).isEqualTo("denied");
    }

    @Test
    void shouldScopeSpecificationDetailListAndCount() {
        final Specification<Account> any = (r, q, cb) -> cb.conjunction();
        final var query = template.query(Account.class).permission("read").where(any);
        assertThat(query.list(Sort.by("id")).get()).extracting(a -> a.id).containsExactly(1L, 2L);
        assertThat(query.count().get()).isEqualTo(2);
        assertThat(query.getDetail(1L).get().isDefined()).isTrue();
        assertThat(query.getDetail(3L).get()).isEmpty();
        assertThat(query.exists().get()).isTrue();
    }

    @Test
    void shouldSupportStringOwnerAndRejectUnregisteredOrInvalidMappings() {
        final var fields = new DataPermissionProperties().resource("account", Account.class,
                mapping -> mapping.setTenant("tenant").setOwner("tenant").setDepartment("departmentId"));
        final DataPermissionTemplate scope = r -> p -> () -> Either.right(
                new DataPermissionQuery("t1", "t1", false, true, Set.of()));
        final var stringOwner = new DataPermissionJpaTemplate(entityManager, scope, fields);
        assertThat(stringOwner.query(Account.class).permission("read").count().get()).isEqualTo(3);
        assertThatThrownBy(() -> template.query(String.class)).isInstanceOf(IllegalArgumentException.class);
        fields.getResources().get("account").setOwner("missing");
        assertThatThrownBy(() -> new DataPermissionJpaTemplate(entityManager, scope, fields))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldNotLoseConditionsByMixingQueryBackendsOrMissingPermission() {
        final var account = new PathBuilder<>(Account.class, "account");
        final Specification<Account> any = (r, q, cb) -> cb.conjunction();
        assertThat(template.query(Account.class).count().isLeft()).isTrue();
        assertThatThrownBy(() -> template.query(Account.class).where(any).where(account.getString("name").eq("own")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> template.query(Account.class).where(account.getString("name").eq("own")).where(any))
                .isInstanceOf(IllegalStateException.class);
        assertThat(template.query(Account.class).permission("read").list(Sort.by("id")).get())
                .extracting(a -> a.id).containsExactly(1L, 2L);
        assertThat(template.query(Account.class).permission("read").exists().get()).isTrue();
    }

    @Test
    void keepsNativeQueriesWorkingInPersistenceUnitsWithoutAnnotatedResources() {
        assertThat(entityManager.createNativeQuery("select count(*) from PermissionAccount", Long.class)
                .getSingleResult()).isEqualTo(4L);
        try (final var counts = entityManager.createNativeQuery("select count(*) from PermissionAccount", Long.class)
                .getResultStream()) {
            assertThat(counts.toList()).containsExactly(4L);
        }
    }

    private DataPermissionJpaTemplate template(DataPermissionQuery scope) {
        return new DataPermissionJpaTemplate(entityManager, r -> p -> () -> Either.right(scope), properties());
    }

    private DataPermissionProperties properties() {
        return new DataPermissionProperties().resource("account", Account.class,
                fields -> fields.setTenant("tenant").setOwner("ownerId").setDepartment("departmentId"));
    }

    /**
     * 最小业务实体；不依赖 Hibernate 租户过滤，以验证显式租户条件。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @Entity(name = "PermissionAccount")
    public static class Account {
        @Id
        public Long id;
        public String tenant;
        public Long ownerId;
        public Long departmentId;
        public String name;
        protected Account() {
        }
        Account(Long id, String tenant, Long ownerId, Long departmentId, String name) {
            this.id = id;
            this.tenant = tenant;
            this.ownerId = ownerId;
            this.departmentId = departmentId;
            this.name = name;
        }
    }
}
