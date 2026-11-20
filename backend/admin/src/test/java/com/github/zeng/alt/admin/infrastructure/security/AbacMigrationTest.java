package com.github.zeng.alt.admin.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 执行真实升级链，验证方法规则保留、HTTP 规则退役以及已删除列不再出现在最终表结构中。
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacMigrationTest {
    @Test
    void upgradesToMethodOnlyPoliciesAndRetainsTenantBoundManagementPermissions() throws Exception {
        try (final var connection = DriverManager.getConnection("jdbc:h2:mem:abac_migration;MODE=PostgreSQL");
                final var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE main_permission (permission_id BIGINT PRIMARY KEY, resource_type VARCHAR(16),
                    code VARCHAR(100), name VARCHAR(100), method VARCHAR(16), path VARCHAR(100), menu_id BIGINT,
                    tenant_by VARCHAR(50), is_enabled BOOLEAN, is_deleted BOOLEAN, created_by VARCHAR(100), created_date TIMESTAMP)
                    """);
            statement.execute("""
                    CREATE TABLE main_policy_rule (policy_rule_id BIGINT PRIMARY KEY, permission_id BIGINT,
                    condition VARCHAR(1000), resource_id BIGINT, is_deleted BOOLEAN DEFAULT FALSE)
                    """);
            statement.execute("""
                    INSERT INTO main_permission (permission_id, resource_type, code, method, path, tenant_by) VALUES
                    (1, 'HTTP', 'PATCH:/v1/user/{id:\\d+}', 'PATCH', '/v1/user/{id:\\d+}', 't001'),
                    (2, 'HTTP', 'GET:/v1/user/info', 'GET', '/v1/user/info', 't002'),
                    (3, 'HTTP', 'log:read', 'GET', '/v1/log/oper/list', 't001')
                    """);
            statement.execute("""
                    INSERT INTO main_policy_rule (policy_rule_id, permission_id, condition) VALUES
                    (1, 1, 'subject[''id''] == resource[''id'']'),
                    (2, 2, 'environment[''hour''] < 24'), (3, 3, 'false')
                    """);
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/changelog/h2/V0026-2026-11-20-abac-execution-scope.sql"));
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/changelog/h2/V0027-2026-11-20-abac-method-only.sql"));
            try (final var rows = statement.executeQuery("SELECT condition, is_deleted FROM main_policy_rule ORDER BY policy_rule_id")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("principal['id'] == resource['id']");
                assertThat(rows.getBoolean(2)).isFalse();
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("env['hour'] < 24");
                assertThat(rows.getBoolean(2)).isFalse();
                assertThat(rows.next()).isTrue();
                assertThat(rows.getBoolean(2)).isTrue();
            }
            try (final var rows = statement.executeQuery("""
                    SELECT tenant_by, COUNT(*) FROM main_permission WHERE code LIKE 'abac:%' GROUP BY tenant_by
                    """)) {
                int tenants = 0;
                while (rows.next()) { assertThat(rows.getInt(2)).isEqualTo(8); tenants++; }
                assertThat(tenants).isEqualTo(2);
            }
            assertThatThrownBy(() -> statement.executeQuery("SELECT execution_scope FROM main_policy_rule"))
                    .isInstanceOf(java.sql.SQLException.class);
            assertThatThrownBy(() -> statement.executeQuery("SELECT resource_id FROM main_policy_rule"))
                    .isInstanceOf(java.sql.SQLException.class);
        }
    }
}
