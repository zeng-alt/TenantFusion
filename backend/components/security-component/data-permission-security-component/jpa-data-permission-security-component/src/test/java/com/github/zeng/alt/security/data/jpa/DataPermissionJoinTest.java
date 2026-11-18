package com.github.zeng.alt.security.data.jpa;

import com.github.zeng.alt.security.data.DataPermissionQuery;
import com.github.zeng.alt.security.data.DataPermissionTemplate;
import com.github.zeng.alt.security.data.jpa.fixture.PermissionJoinFixtures.JoinQueries;
import com.github.zeng.alt.security.data.jpa.fixture.PermissionJoinFixtures;
import com.github.zeng.alt.security.data.jpa.fixture.PermissionJpaFixtures.SqlRecorder;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;

import static com.github.zeng.alt.security.data.jpa.DataPermissionJpaTestSupport.runner;
import static com.github.zeng.alt.security.data.jpa.DataPermissionJpaTestSupport.seed;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 关联 JOIN 不得绕过实体范围，LEFT JOIN 保留左侧记录，分页内容/count 同步过滤。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class DataPermissionJoinTest {
    @Test
    void filtersExplicitEntityJoinsAndAssociationProjections() {
        joinRunner().run(context -> {
            final JoinQueries queries = context.getBean(JoinQueries.class);
            assertThat(queries.explicit()).containsExactly(1L, 2L);
            assertThat(queries.association()).containsExactly(1L, 2L);
        });
    }

    @Test
    void keepsLeftRowsWhenTheJoinedRowsAreFilteredOut() {
        joinRunner().run(context -> {
            assertThat(context.getBean(JoinQueries.class).left()).containsExactly(
                    new Object[] {1L, 1L}, new Object[] {1L, 2L}, new Object[] {2L, null});
        });
    }

    @Test
    void filtersFetchJoinedCollectionsBeforeTheyAreInitialized() {
        joinRunner().run(context -> {
            final var folders = context.getBean(JoinQueries.class).fetch();
            assertThat(folders).hasSize(2);
            assertThat(folders.getFirst().documents).extracting(document -> document.id)
                    .containsExactlyInAnyOrder(1L, 2L, 5L);
            assertThat(folders.getLast().documents).isEmpty();
        });
    }

    @Test
    void filtersJoinPagesAndTheirCountQueryInSql() {
        joinRunner().run(context -> {
            final var queries = context.getBean(JoinQueries.class);
            final var page = queries.page(PageRequest.of(0, 1));
            assertThat(page.getContent()).extracting(document -> document.id).containsExactly(1L);
            assertThat(page.getTotalElements()).isEqualTo(2);
            final var sql = context.getBean(SqlRecorder.class).statements;
            assertThat(sql).hasSize(2).allSatisfy(statement -> assertThat(statement)
                    .contains("tenant_key", "owner_key", "department_key"));
            assertThat(queries.page(PageRequest.of(1, 1)).getContent())
                    .extracting(document -> document.id).containsExactly(2L);
        });
    }

    @Test
    void appliesFiltersToManyToManyTargetTables() {
        joinRunner().run(context -> assertThat(context.getBean(JoinQueries.class).manyToMany())
                .containsExactly(1L, 2L));
    }

    @Test
    void keepsTenantIsolationForAllAndLeftRowsForAnEmptyScope() {
        joinRunner().withBean(DataPermissionTemplate.class, () -> resource -> permission -> () -> Either.right(
                new DataPermissionQuery("t1", "7", true, false, Set.of()))).run(context -> {
            assertThat(context.getBean(JoinQueries.class).association()).containsExactly(1L, 2L, 3L);
        });
        joinRunner().withBean(DataPermissionTemplate.class, () -> resource -> permission -> () -> Either.right(
                new DataPermissionQuery("t1", "7", false, false, Set.of()))).run(context -> {
            final var queries = context.getBean(JoinQueries.class);
            assertThat(queries.association()).isEmpty();
            assertThat(queries.left()).containsExactly(new Object[] {1L, null}, new Object[] {2L, null});
        });
    }

    private ApplicationContextRunner joinRunner() {
        return runner().withUserConfiguration(PermissionJoinFixtures.class).withInitializer(context -> context.addApplicationListener(event -> {
                    if (event instanceof org.springframework.context.event.ContextRefreshedEvent) {
                        final var jdbc = new JdbcTemplate(context.getBean(javax.sql.DataSource.class));
                        seed(jdbc);
                        jdbc.update("insert into permission_folder(id) values (1), (2)");
                        jdbc.update("update permission_document set folder_id = 1");
                        jdbc.update("insert into permission_group(id) values (1), (2)");
                        jdbc.update("""
                                insert into permission_group_document(group_id, document_id)
                                values (1, 1), (1, 2), (1, 3), (1, 4), (2, 1)
                                """);
                    }
                }));
    }

}
