package com.github.zeng.alt.tenant.schema;

import com.github.zeng.alt.tenant.api.TenantDialect;
import com.github.zeng.alt.tenant.api.TenantRouting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 用真实 H2 验证 schema 的切换与复位。
 * <p>
 * 复位这一步是模式级隔离里最危险的环节：带着上一个租户 schema 的连接还进池，
 * 下一个租户就会读写到别人的数据，所以专门覆盖。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
class SchemaConnectionCustomizerTest {

    private static final String URL = "jdbc:h2:mem:tenant_schema_test;DB_CLOSE_DELAY=-1";

    /** 内联一个 H2 方言，避免测试依赖 h2-tenant-component 模块 */
    private static final TenantDialect H2 = new TenantDialect() {
        @Override
        public String getName() {
            return "h2";
        }

        @Override
        public String schemaSwitchSql(String schema) {
            return "SET SCHEMA " + requireSafeIdentifier(schema);
        }

        @Override
        public String schemaResetSql() {
            return "SET SCHEMA PUBLIC";
        }

        @Override
        public String quoteIdentifier(String identifier) {
            return "\"" + identifier.replace("\"", "\"\"") + "\"";
        }
    };

    private static Connection keepAlive;

    @BeforeAll
    static void createSchemas() throws SQLException {
        keepAlive = DriverManager.getConnection(URL, "sa", "");
        try (Statement st = keepAlive.createStatement()) {
            st.execute("CREATE SCHEMA IF NOT EXISTS tenant_a");
            st.execute("CREATE SCHEMA IF NOT EXISTS tenant_b");
            st.execute("CREATE TABLE tenant_a.doc(id INT PRIMARY KEY, title VARCHAR(50))");
            st.execute("CREATE TABLE tenant_b.doc(id INT PRIMARY KEY, title VARCHAR(50))");
            st.execute("INSERT INTO tenant_a.doc VALUES (1, 'a 的数据')");
            st.execute("INSERT INTO tenant_b.doc VALUES (2, 'b 的数据')");
        }
    }

    @AfterAll
    static void close() throws SQLException {
        if (keepAlive != null) {
            keepAlive.close();
        }
    }

    @Test
    @DisplayName("切换后不带 schema 前缀的查询落到租户自己的 schema")
    void switchesToTenantSchema() throws SQLException {
        SchemaConnectionCustomizer customizer = new SchemaConnectionCustomizer(H2);
        try (Connection connection = DriverManager.getConnection(URL, "sa", "")) {
            customizer.apply(connection, routing("tenant_a"));
            assertThat(titleOfSingleRow(connection)).isEqualTo("a 的数据");

            customizer.reset(connection, routing("tenant_a"));
            customizer.apply(connection, routing("tenant_b"));
            assertThat(titleOfSingleRow(connection)).isEqualTo("b 的数据");
        }
    }

    @Test
    @DisplayName("复位后回到 PUBLIC，不再残留上一个租户的 schema")
    void resetsToPublic() throws SQLException {
        SchemaConnectionCustomizer customizer = new SchemaConnectionCustomizer(H2);
        try (Connection connection = DriverManager.getConnection(URL, "sa", "")) {
            customizer.apply(connection, routing("tenant_a"));
            customizer.reset(connection, routing("tenant_a"));

            assertThat(currentSchema(connection)).isEqualTo("PUBLIC");
            // PUBLIC 下没有 doc 表，未带前缀的查询必须失败——若这里能查通，说明 schema 没复位干净
            assertThatThrownBy(() -> titleOfSingleRow(connection))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    @DisplayName("未配置 schema 的路由不介入")
    void inactiveWithoutSchema() {
        SchemaConnectionCustomizer customizer = new SchemaConnectionCustomizer(H2);
        assertThat(customizer.supports(new TenantRouting("t", null, null, null, true))).isFalse();
        assertThat(customizer.supports(routing("tenant_a"))).isTrue();
    }

    private static TenantRouting routing(String schema) {
        return new TenantRouting("t", null, schema, null, false);
    }

    private static String titleOfSingleRow(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("select title from doc")) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private static String currentSchema(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("select schema()")) {
            return rs.next() ? rs.getString(1) : null;
        }
    }
}
