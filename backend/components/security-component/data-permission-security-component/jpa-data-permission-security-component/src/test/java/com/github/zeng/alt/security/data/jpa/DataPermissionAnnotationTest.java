package com.github.zeng.alt.security.data.jpa;

import com.github.zeng.alt.security.data.DataPermissionQuery;
import com.github.zeng.alt.security.data.DataPermissionTemplate;
import com.github.zeng.alt.security.data.jpa.fixture.PermissionJpaFixtures.ClassQueries;
import com.github.zeng.alt.security.data.jpa.fixture.PermissionJpaFixtures.Documents;
import com.github.zeng.alt.security.data.jpa.fixture.PermissionJpaFixtures.InterfaceQueries;
import com.github.zeng.alt.security.data.jpa.fixture.PermissionJpaFixtures.Queries;
import com.github.zeng.alt.security.data.jpa.fixture.PermissionJpaFixtures.SqlRecorder;
import io.vavr.control.Either;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.zeng.alt.security.data.jpa.DataPermissionJpaTestSupport.runner;
import static com.github.zeng.alt.security.data.jpa.DataPermissionJpaTestSupport.scope;
import static com.github.zeng.alt.security.data.jpa.DataPermissionJpaTestSupport.seed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 注解驱动的 SQL 过滤在集合、分页和计数之前生效。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class DataPermissionAnnotationTest {
    @Test
    void filtersOrdinaryRepositoryListsAndJpqlAndQuerydsl() {
        runner().run(context -> {
            seed(context.getBean(JdbcTemplate.class));
            final Queries queries = context.getBean(Queries.class);
            assertThat(queries.list()).extracting(document -> document.id).containsExactly(1L, 2L);
            assertThat(queries.jpql()).extracting(document -> document.id).containsExactly(1L, 2L);
            assertThat(queries.querydsl()).extracting(document -> document.id).containsExactly(1L, 2L);
        });
    }

    @Test
    void appliesTheSameScopeToPageContentAndTotalCount() {
        final AtomicInteger resolutions = new AtomicInteger();
        runner().withBean(DataPermissionTemplate.class, () -> resource -> permission -> () -> {
            resolutions.incrementAndGet();
            return Either.right(scope());
        }).run(context -> {
            seed(context.getBean(JdbcTemplate.class));
            final Queries queries = context.getBean(Queries.class);
            final var page = queries.page(PageRequest.of(0, 1, Sort.by("id")));
            assertThat(page.getContent()).extracting(document -> document.id).containsExactly(1L);
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(resolutions).hasValue(1);
            final var sql = context.getBean(SqlRecorder.class).statements;
            assertThat(sql).hasSize(2);
            assertThat(sql).allSatisfy(statement -> assertThat(statement)
                    .contains("tenant_key", "owner_key", "department_key", "?"));
            assertThat(sql.getLast().toLowerCase()).contains("count(");
            assertThat(queries.count()).isEqualTo(2);
            assertThat(queries.page(PageRequest.of(1, 1, Sort.by("id"))).getContent())
                    .extracting(document -> document.id).containsExactly(2L);
        });
    }

    @Test
    void intersectsSpecificationOrConditionsAndKeepsAllInsideTheTenant() {
        final AtomicReference<DataPermissionQuery> current = new AtomicReference<>(scope());
        runner().withBean(DataPermissionTemplate.class,
                () -> resource -> permission -> () -> Either.right(current.get())).run(context -> {
            seed(context.getBean(JdbcTemplate.class));
            final Queries queries = context.getBean(Queries.class);
            assertThat(queries.specification(PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
            current.set(new DataPermissionQuery("t1", "7", true, false, Set.of()));
            assertThat(queries.count()).isEqualTo(3);
            current.set(new DataPermissionQuery("t1", "7", false, false, Set.of()));
            assertThat(queries.list()).isEmpty();
            assertThat(queries.page(PageRequest.of(0, 1)).getTotalElements()).isZero();
        });
    }

    @Test
    void propagatesAuthorizationAndBusinessFailuresWithoutLeakingTheScope() {
        runner().run(context -> {
            seed(context.getBean(JdbcTemplate.class));
            final Queries queries = context.getBean(Queries.class);
            assertThatThrownBy(queries::fail).hasMessage("business failure");
            assertThat(queries.count()).isEqualTo(2);
        });
        runner().withBean(DataPermissionTemplate.class,
                () -> resource -> permission -> () -> Either.left("denied")).run(context -> {
            assertThatThrownBy(() -> context.getBean(Queries.class).count()).hasMessageContaining("denied");
        });
    }

    @Test
    void filtersFindByIdAndExistsAndRejectsUnscopedReads() {
        runner().run(context -> {
            seed(context.getBean(JdbcTemplate.class));
            final Queries queries = context.getBean(Queries.class);
            assertThat(queries.detail(1L)).isPresent();
            assertThat(queries.detail(3L)).isEmpty();
            assertThat(queries.detail(4L)).isEmpty();
            assertThat(queries.exists(1L)).isTrue();
            assertThat(queries.exists(3L)).isFalse();
            assertThatThrownBy(() -> context.getBean(Documents.class).findByEnabledTrueOrderByIdAsc())
                    .hasStackTraceContaining("Missing @DataPermission scope");
            assertThat(DataPermissionContext.active()).isFalse();
        });
    }

    @Test
    void rejectsNativeSqlAndBulkWritesInsteadOfSilentlySkippingTheFilter() {
        runner().run(context -> {
            seed(context.getBean(JdbcTemplate.class));
            final Queries queries = context.getBean(Queries.class);
            assertThatThrownBy(queries::nativeCount).hasStackTraceContaining("Native SQL");
            assertThatThrownBy(queries::bulkDelete).hasStackTraceContaining("read-only");
            assertThat(queries.count()).isEqualTo(2);
        });
    }

    @Test
    void doesNotReturnCachedEntitiesAfterLeavingTheScope() {
        runner().run(context -> {
            seed(context.getBean(JdbcTemplate.class));
            final var transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            transaction.executeWithoutResult(status -> {
                assertThat(context.getBean(Queries.class).detail(1L)).isPresent();
                assertThatThrownBy(() -> context.getBean(Documents.class).findById(1L))
                        .hasStackTraceContaining("Missing @DataPermission scope");
                status.setRollbackOnly();
            });
            assertThat(DataPermissionContext.active()).isFalse();
        });
    }

    @Test
    void supportsClassAndInterfaceAnnotationsAndRestoresNestedCalls() {
        runner().run(context -> {
            seed(context.getBean(JdbcTemplate.class));
            final InterfaceQueries fromInterface = context.getBean(InterfaceQueries.class);
            final ClassQueries fromClass = context.getBean(ClassQueries.class);
            assertThat(fromInterface.count()).isEqualTo(2);
            assertThat(fromClass.count()).isEqualTo(2);
            assertThat(fromClass.nested(fromInterface)).isEqualTo(4);
            assertThat(DataPermissionContext.active()).isFalse();
        });
    }

    @Test
    void reevaluatesRulesAndBindsDifferentDepartmentListSizesOnTheNextCall() {
        final AtomicReference<DataPermissionQuery> current = new AtomicReference<>(scope());
        runner().withBean(DataPermissionTemplate.class,
                () -> resource -> permission -> () -> Either.right(current.get())).run(context -> {
            seed(context.getBean(JdbcTemplate.class));
            final Queries queries = context.getBean(Queries.class);
            assertThat(queries.list()).hasSize(2);
            current.set(new DataPermissionQuery("t1", "7", false, false, Set.of(10L, 20L, 30L)));
            assertThat(queries.list()).hasSize(3);
            current.set(new DataPermissionQuery("t2", "7", false, false, Set.of(20L)));
            assertThat(queries.list()).extracting(document -> document.id).containsExactly(4L);
            assertThat(queries.count()).isEqualTo(1);
        });
    }

    @Test
    void rejectsMissingSubjectsInsteadOfTreatingThemAsAll() {
        runner().withBean(DataPermissionTemplate.class, () -> resource -> permission -> () -> Either.right(
                new DataPermissionQuery("", "7", true, false, Set.of()))).run(context -> {
            assertThatThrownBy(() -> context.getBean(Queries.class).count())
                    .hasStackTraceContaining("requires a tenant and user ID");
            assertThat(DataPermissionContext.active()).isFalse();
        });
    }

    @Test
    void rechecksCachedEntitiesInsideScopedMethodsAndRejectsAnIncompatibleExistingContext() {
        final AtomicReference<DataPermissionQuery> current = new AtomicReference<>(scope());
        runner().withBean(DataPermissionTemplate.class,
                () -> resource -> permission -> () -> Either.right(current.get())).run(context -> {
            seed(context.getBean(JdbcTemplate.class));
            final Queries queries = context.getBean(Queries.class);
            assertThat(queries.cachedDetail(1L).id).isEqualTo(1L);
            assertThat(queries.cachedDetail(2L).id).isEqualTo(2L);
            final var transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            transaction.executeWithoutResult(status -> {
                current.set(new DataPermissionQuery("t1", "7", true, false, Set.of()));
                assertThat(queries.list()).hasSize(3);
                current.set(scope());
                assertThatThrownBy(queries::count).hasStackTraceContaining("outside the permission scope");
                status.setRollbackOnly();
            });
            assertThat(DataPermissionContext.active()).isFalse();
        });
    }

    @Test
    void permitsTrustedPolicyReadsAndThenRestoresNormalFiltering() {
        final AtomicReference<Documents> documents = new AtomicReference<>();
        final AtomicInteger policyRows = new AtomicInteger();
        runner().withBean(DataPermissionTemplate.class, () -> resource -> permission -> () -> {
            policyRows.set((int) documents.get().count());
            return Either.right(scope());
        }).run(context -> {
            documents.set(context.getBean(Documents.class));
            seed(context.getBean(JdbcTemplate.class));
            assertThat(context.getBean(Queries.class).count()).isEqualTo(2);
            assertThat(policyRows).hasValue(5);
            assertThat(DataPermissionContext.policyRead()).isFalse();
        });
    }

    @Test
    void rejectsNativePlansCreatedOutsideTheScopeAndReusedInsideIt() {
        runner().run(context -> {
            seed(context.getBean(JdbcTemplate.class));
            try (final var entityManager = context.getBean(EntityManagerFactory.class).createEntityManager()) {
                assertThatThrownBy(() -> entityManager.createNativeQuery(
                        "select count(*) from permission_document", Long.class).getResultList())
                        .hasStackTraceContaining("Native SQL");
            }
            assertThatThrownBy(() -> context.getBean(Queries.class).nativeCount()).hasStackTraceContaining("Native SQL");
            assertThat(context.getBean(Queries.class).count()).isEqualTo(2);
        });
    }

    @Test
    void refusesUnfilteredLazyCollectionsOfProtectedEntities() {
        runner().run(context -> {
            final JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            seed(jdbc);
            jdbc.update("insert into permission_folder(id) values (1)");
            jdbc.update("update permission_document set folder_id = 1");
            assertThatThrownBy(() -> context.getBean(Queries.class).associationCount())
                    .hasStackTraceContaining("Collection associations");
            assertThat(context.getBean(Queries.class).count()).isEqualTo(2);
        });
    }

}
