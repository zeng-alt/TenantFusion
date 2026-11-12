package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.tenantstore.TenantStoreRuntimeHints;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import java.sql.DriverManager;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证租户库 SQL 的 Native 资源提示及显式清单保留迁移身份。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class TenantStoreRuntimeHintsTest {

    @Test
    void includesBothDialectMigrationFiles() {
        final RuntimeHints hints = new RuntimeHints();
        new TenantStoreRuntimeHints().registerHints(hints, getClass().getClassLoader());

        for (final String dialect : new String[]{"h2", "pg"}) {
            final String directory = "db/changelog/tenant/" + dialect;
            assertThat(RuntimeHintsPredicates.resource()
                    .forResource(directory + "/V0001-2026-10-02-init-tenant-store.sql").test(hints)).isTrue();
        }
        assertThat(hints.resources().resourcePatternHints().flatMap(group -> group.getIncludes().stream())
                .map(pattern -> pattern.getPattern()))
                .contains("db/changelog/db.changelog-tenant.yaml", "db/changelog/tenant/**");
    }

    @ParameterizedTest
    @ValueSource(strings = {"h2", "pg"})
    void explicitManifestKeepsExistingChangeSetPaths(String dialect) throws Exception {
        try (final var connection = DriverManager.getConnection("jdbc:h2:mem:tenant_manifest_" + dialect)) {
            final var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (final var liquibase = new Liquibase("db/changelog/db.changelog-tenant.yaml",
                    new ClassLoaderResourceAccessor(), database)) {
                liquibase.setChangeLogParameter("tenantChangelogDir", "tenant/" + dialect);
                final var changes = liquibase.getDatabaseChangeLog().getChangeSets();
                assertThat(changes).extracting(change -> change.getAuthor() + ":" + change.getId())
                        .containsExactly("zeng:init-tenant-store", "zeng:seed-demo-tenant", "zeng:seed-hybrid-demo-tenants");
                assertThat(changes)
                        .extracting(change -> change.getFilePath())
                        .containsExactly(
                                "db/changelog/tenant/" + dialect + "/V0001-2026-10-02-init-tenant-store.sql",
                                "db/changelog/tenant/" + dialect + "/V0002-2026-10-02-seed-demo-tenant.sql",
                                "db/changelog/tenant/" + dialect + "/V0003-2026-10-02-seed-hybrid-demo-tenants.sql"
                        );
            }
        }
    }

}
